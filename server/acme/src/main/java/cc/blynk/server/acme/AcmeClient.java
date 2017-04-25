package cc.blynk.server.acme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.*;
import java.net.URI;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

/**
 * A simple client test tool.
 * <p>
 * Pass the names of the domains as parameters.
 */
public class AcmeClient {

    private static final Logger log = LogManager.getLogger(AcmeClient.class);

    // File name of the User Key Pair
    private static final File USER_KEY_FILE = new File("user.key");

    // File name of the Domain Key Pair
    private static final File DOMAIN_KEY_FILE = new File("domain.key");

    // File name of the CSR
    private static final File DOMAIN_CSR_FILE = new File("domain.csr");

    // File name of the signed certificate
    private static final File DOMAIN_CHAIN_FILE = new File("domain-chain.crt");

    // RSA key size of generated key pairs
    private static final int KEY_SIZE = 2048;

    /**
     * Generates a certificate for the given domains. Also takes care for the registration
     * process.
     *
     * @param domains
     *            Domains to get a common certificate for
     */
    public void fetchCertificate(Collection<String> domains) throws IOException, AcmeException {
        // Load the user key file. If there is no key file, create a new one.
        // Keep this key pair in a safe place! In a production environment, you will not be
        // able to access your account again if you should lose the key pair.
        KeyPair userKeyPair = loadOrCreateKeyPair(USER_KEY_FILE);

        // Create a session for Let's Encrypt.
        // Use "acme://letsencrypt.org" for production server
        Session session = new Session("acme://letsencrypt.org/staging", userKeyPair);

        // Get the Registration to the account.
        // If there is no account yet, create a new one.
        Registration reg = findOrRegisterAccount(session);

        // Separately authorize every requested domain.
        for (String domain : domains) {
            authorize(reg, domain);
        }

        // Load or create a key pair for the domains. This should not be the userKeyPair!
        KeyPair domainKeyPair = loadOrCreateKeyPair(DOMAIN_KEY_FILE);

        // Generate a CSR for all of the domains, and sign it with the domain key pair.
        CSRBuilder csrb = new CSRBuilder();
        csrb.addDomains(domains);
        csrb.sign(domainKeyPair);

        // Write the CSR to a file, for later use.
        try (Writer out = new FileWriter(DOMAIN_CSR_FILE)) {
            csrb.write(out);
        }

        // Now request a signed certificate.
        Certificate certificate = reg.requestCertificate(csrb.getEncoded());

        log.info("Success! The certificate for domains " + domains + " has been generated!");
        log.info("Certificate URI: " + certificate.getLocation());

        // Download the leaf certificate and certificate chain.
        X509Certificate cert = certificate.download();
        X509Certificate[] chain = certificate.downloadChain();

        // Write a combined file containing the certificate and chain.
        try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE)) {
            CertificateUtils.writeX509CertificateChain(fw, cert, chain);
        }

        // That's all! Configure your web server to use the DOMAIN_KEY_FILE and
        // DOMAIN_CHAIN_FILE for the requested domans.
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
    private Registration findOrRegisterAccount(Session session) throws AcmeException {
        Registration reg;

        try {
            // Try to create a new Registration.
            reg = new RegistrationBuilder().create(session);
            log.info("Registered a new user, URI: " + reg.getLocation());

            // This is a new account. Let the user accept the Terms of Service.
            // We won't be able to authorize domains until the ToS is accepted.
            URI agreement = reg.getAgreement();
            log.info("Terms of Service: " + agreement);
            reg.modify().setAgreement(agreement).commit();

        } catch (AcmeConflictException ex) {
            // The Key Pair is already registered. getLocation() contains the
            // URL of the existing registration's location. Bind it to the session.
            reg = Registration.bind(session, ex.getLocation());
            log.info("Account does already exist, URI: " + reg.getLocation(), ex);
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
        Challenge challenge = httpChallenge(auth, domain);

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
    public Challenge httpChallenge(Authorization auth, String domain) throws AcmeException {
        // Find a single http-01 challenge
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
        if (challenge == null) {
            throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
        }

        // Output the challenge, wait for acknowledge...
        log.info("Please create a file in your web server's base directory.");
        log.info("It must be reachable at: http://" + domain + "/.well-known/acme-challenge/" + challenge.getToken());
        log.info("File name: " + challenge.getToken());
        log.info("Content: " + challenge.getAuthorization());
        log.info("The file must not contain any leading or trailing whitespaces or line breaks!");
        log.info("If you're ready, dismiss the dialog...");

        log.info("Please create a file in your web server's base directory.");
        log.info("http://{}/.well-known/acme-challenge/{}", domain, challenge.getToken());
        log.info("Content: {}", challenge.getAuthorization());

        return challenge;
    }


    /**
     * Invokes this example.
     *
     * @param args
     *            Domains to get a certificate for
     */
    public static void main(String... args) {
        if (args.length == 0) {
            System.err.println("Usage: AcmeClient <domain>...");
            System.exit(1);
        }

        log.info("Starting up...");

        Security.addProvider(new BouncyCastleProvider());

        Collection<String> domains = Arrays.asList(args);
        try {
            AcmeClient ct = new AcmeClient();
            ct.fetchCertificate(domains);
        } catch (Exception ex) {
            log.error("Failed to get a certificate for domains " + domains, ex);
        }
    }

}
