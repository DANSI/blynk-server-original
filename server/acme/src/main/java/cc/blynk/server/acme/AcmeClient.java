package cc.blynk.server.acme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 * A simple client test tool.
 * <p>
 * Pass the names of the domains as parameters.
 */
public class AcmeClient {

    private static final Logger log = LogManager.getLogger(AcmeClient.class);

    // File name of the User Key Pair
    public static final File USER_KEY_FILE = new File("user.pem");

    // File name of the Domain Key Pair
    public static final File DOMAIN_KEY_FILE = new File("privkey.pem");

    // File name of the signed certificate
    public static final File DOMAIN_CHAIN_FILE = new File("fullchain.crt");

    private static final String PRODUCTION = "acme://letsencrypt.org";

    // RSA key size of generated key pairs
    private static final int KEY_SIZE = 2048;

    private final String letsEncryptUrl;
    private final String email;
    private final String host;
    private final ContentHolder contentHolder;

    public AcmeClient(String email, String host, ContentHolder contentHolder) {
        this(PRODUCTION, email, host, contentHolder);
    }

    public AcmeClient(String letsEncryptUrl, String email, String host, ContentHolder contentHolder) {
        this.letsEncryptUrl = letsEncryptUrl;
        this.email = email;
        this.host = host;
        this.contentHolder = contentHolder;
    }

    public boolean requestCertificate() throws Exception {
        log.info("Starting up certificate retrieval process for host {} and email {}.", host, email);
        return fetchCertificate(email, host);
    }

    /**
     * Generates a certificate for the given domains. Also takes care for the registration
     * process.
     *
     * @param domain
     *            Domains to get a common certificate for
     */
    public boolean fetchCertificate(String contact, String domain) throws IOException, AcmeException {
        // Load the user key file. If there is no key file, create a new one.
        // Keep this key pair in a safe place! In a production environment, you will not be
        // able to access your account again if you should lose the key pair.
        KeyPair userKeyPair = loadOrCreateKeyPair(USER_KEY_FILE);

        Session session = new Session(letsEncryptUrl, userKeyPair);

        // Get the Registration to the account.
        // If there is no account yet, create a new one.
        Registration reg = findOrRegisterAccount(session, contact);

        // Separately authorize every requested domain.
        authorize(reg, domain);

        // Load or create a key pair for the domains. This should not be the userKeyPair!
        KeyPair domainKeyPair = loadOrCreateKeyPair(DOMAIN_KEY_FILE);

        // Generate a CSR for all of the domains, and sign it with the domain key pair.
        CSRBuilder csrb = new CSRBuilder();
        csrb.addDomain(domain);
        csrb.setOrganization("Blynk Inc.");
        csrb.sign(domainKeyPair);

        // Write the CSR to a file, for later use.
        //try (Writer out = new FileWriter(DOMAIN_CSR_FILE)) {
        //    csrb.write(out);
        //}

        // Now request a signed certificate.
        Certificate certificate = reg.requestCertificate(csrb.getEncoded());

        // Download the leaf certificate and certificate chain.
        X509Certificate cert = certificate.download();
        X509Certificate[] chain = certificate.downloadChain();

        // Write a combined file containing the certificate and chain.
        try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE)) {
            CertificateUtils.writeX509CertificateChain(fw, cert, chain);
        }

        return true;
    }

    /**
     * Loads a key pair from specified file. If the file does not exist,
     * a new key pair is generated and saved.
     *
     * @return {@link KeyPair}.
     */
    private KeyPair loadOrCreateKeyPair(File file) throws IOException {
        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                return KeyPairUtils.readKeyPair(fr);
            }
        } else {
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
            try (FileWriter fw = new FileWriter(file)) {
                KeyPairUtils.writeKeyPair(domainKeyPair, fw);
            }
            return domainKeyPair;
        }
    }

    /**
     * Finds your {@link Registration} at the ACME server. It will be found by your user's
     * public key. If your key is not known to the server yet, a new registration will be
     * created.
     * <p>
     * This is a simple way of finding your {@link Registration}. A better way is to get
     * the URI of your new registration with {@link Registration#getLocation()} and store
     * it somewhere. If you need to get access to your account later, reconnect to it via
     * {@link Registration#bind(Session, URI)} by using the stored location.
     *
     * @param session
     *            {@link Session} to bind with
     * @return {@link Registration} connected to your account
     */
    private Registration findOrRegisterAccount(Session session, String contact) throws AcmeException {
        Registration reg;

        try {
            // Try to create a new Registration.
            reg = new RegistrationBuilder().addContact("mailto:" + contact).create(session);
            log.info("Registered a new user, URI: " + reg.getLocation());

            // This is a new account. Let the user accept the Terms of Service.
            // We won't be able to authorize domains until the ToS is accepted.
            URI agreement = reg.getAgreement();
            reg.modify().setAgreement(agreement).commit();

        } catch (AcmeConflictException ex) {
            // The Key Pair is already registered. getLocation() contains the
            // URL of the existing registration's location. Bind it to the session.
            reg = Registration.bind(session, ex.getLocation());
            log.info("Account does already exist, URI: " + reg.getLocation());
            log.debug(ex);
        }

        return reg;
    }

    /**
     * Authorize a domain. It will be associated with your account, so you will be able to
     * retrieve a signed certificate for the domain later.
     * <p>
     * You need separate authorizations for subdomains (e.g. "www" subdomain). Wildcard
     * certificates are not currently supported.
     *
     * @param reg
     *            {@link Registration} of your account
     * @param domain
     *            Name of the domain to authorize
     */
    private void authorize(Registration reg, String domain) throws AcmeException {
        // Authorize the domain.
        Authorization auth = reg.authorizeDomain(domain);
        log.info("Authorization for domain " + domain);

        // Find the desired challenge and prepare it.
        Http01Challenge challenge = httpChallenge(auth, domain);
        contentHolder.content = challenge.getAuthorization();

        // If the challenge is already verified, there's no need to execute it again.
        if (challenge.getStatus() == Status.VALID) {
            return;
        }

        // Now trigger the challenge.
        challenge.trigger();

        // Poll for the challenge to complete.
        try {
            int attempts = 10;
            while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
                // Did the authorization fail?
                if (challenge.getStatus() == Status.INVALID) {
                    throw new AcmeException("Challenge failed... Giving up.");
                }

                // Wait for a few seconds
                Thread.sleep(3000L);

                // Then update the status
                challenge.update();
            }
        } catch (InterruptedException ex) {
            log.error("interrupted", ex);
            Thread.currentThread().interrupt();
        }

        // All reattempts are used up and there is still no valid authorization?
        if (challenge.getStatus() != Status.VALID) {
            throw new AcmeException("Failed to pass the challenge for domain " + domain + ", ... Giving up.");
        }
    }

    /**
     * Prepares a HTTP challenge.
     * <p>
     * The verification of this challenge expects a file with a certain content to be
     * reachable at a given path under the domain to be tested.
     * <p>
     * This example outputs instructions that need to be executed manually. In a
     * production environment, you would rather generate this file automatically, or maybe
     * use a servlet that returns {@link Http01Challenge#getAuthorization()}.
     *
     * @param auth
     *            {@link Authorization} to find the challenge in
     * @param domain
     *            Domain name to be authorized
     * @return {@link Challenge} to verify
     */
    public Http01Challenge httpChallenge(Authorization auth, String domain) throws AcmeException {
        // Find a single http-01 challenge
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
        if (challenge == null) {
            throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
        }

        // Output the challenge, wait for acknowledge...
        log.debug("http://{}/.well-known/acme-challenge/{}", domain, challenge.getToken());
        log.debug("Content: {}", challenge.getAuthorization());

        return challenge;
    }



}
