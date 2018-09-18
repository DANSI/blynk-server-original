package cc.blynk.server.acme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;

/**
 * A simple client test tool.
 * <p>
 * Pass the names of the domains as parameters.
 */
public class AcmeClient {

    private static final Logger log = LogManager.getLogger(AcmeClient.class);

    // File name of the User Key Pair
    private static final File USER_KEY_FILE = new File("user.pem");

    // File name of the Domain Key Pair
    public static final File DOMAIN_KEY_FILE = new File("privkey.pem");

    // File name of the signed certificate
    public static final File DOMAIN_CHAIN_FILE = new File("fullchain.crt");

    private static final String PRODUCTION = "acme://letsencrypt.org";

    // RSA key size of generated key pairs
    private static final int KEY_SIZE = 2048;
    private static final int ATTEMPTS = 10;
    private static final long WAIT_MILLIS = 3000L;

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

    public void requestCertificate() throws Exception {
        log.info("Starting up certificate retrieval process for host {} and email {}.", host, email);
        fetchCertificate(email, host);
    }

    /**
     * Generates a certificate for the given domains. Also takes care for the registration
     * process.
     *
     * @param domain
     *            Domains to get a common certificate for
     */
    private void fetchCertificate(String contact, String domain) throws IOException, AcmeException {
        // Load the user key file. If there is no key file, create a new one.
        // Keep this key pair in a safe place! In a production environment, you will not be
        // able to access your account again if you should lose the key pair.
        KeyPair userKeyPair = loadOrCreateKeyPair(USER_KEY_FILE);

        Session session = new Session(letsEncryptUrl);

        // Get the Account.
        // If there is no account yet, create a new one.
        Account account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(userKeyPair)
                .addEmail(contact)
                .create(session);
        log.info("Registered a new user, URL: {}", account.getLocation());

        // Load or create a key pair for the domains. This should not be the userKeyPair!
        KeyPair domainKeyPair = loadOrCreateKeyPair(DOMAIN_KEY_FILE);

        // Order the certificate
        Order order = account.newOrder().domain(domain).create();

        // Perform all required authorizations
        for (Authorization auth : order.getAuthorizations()) {
            authorize(auth);
        }

        // Generate a CSR for all of the domains, and sign it with the domain key pair.
        CSRBuilder csrb = new CSRBuilder();
        csrb.addDomain(domain);
        csrb.setOrganization("Blynk Inc.");
        csrb.sign(domainKeyPair);

        // Order the certificate
        order.execute(csrb.getEncoded());

        // Wait for the order to complete
        try {
            int attempts = ATTEMPTS;
            while (order.getStatus() != Status.VALID && attempts-- > 0) {
                if (order.getStatus() == Status.INVALID) {
                    throw new AcmeException("Order failed... Giving up.");
                }
                Thread.sleep(WAIT_MILLIS);
                order.update();
            }
        } catch (InterruptedException ex) {
            log.error("interrupted", ex);
        }

        Certificate certificate = order.getCertificate();

        if (certificate != null) {
            try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE)) {
                certificate.writeCertificate(fw);
            }
            log.info("Overriding certificate. Expiration date is : {}", certificate.getCertificate().getNotAfter());
        }
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
     * Authorize a domain. It will be associated with your account, so you will be able to
     * retrieve a signed certificate for the domain later.
     *
     * @param auth
     *            {@link Authorization} to perform
     */
    private void authorize(Authorization auth) throws AcmeException {
        log.info("Starting authorization for domain {}", auth.getIdentifier().getDomain());

        // Find the desired challenge and prepare it.
        Http01Challenge challenge = httpChallenge(auth);

        if (challenge == null) {
            throw new AcmeException("No challenge found");
        }

        contentHolder.content = challenge.getAuthorization();

        // If the challenge is already verified, there's no need to execute it again.
        if (challenge.getStatus() == Status.VALID) {
            return;
        }

        // Now trigger the challenge.
        challenge.trigger();

        // Poll for the challenge to complete.
        try {
            int attempts = ATTEMPTS;
            while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
                if (challenge.getStatus() == Status.INVALID) {
                    throw new AcmeException("Challenge failed... Giving up.");
                }
                Thread.sleep(WAIT_MILLIS);
                challenge.update();
            }
        } catch (InterruptedException ex) {
            log.error("interrupted", ex);
            return;
        }

        // All reattempts are used up and there is still no valid authorization?
        if (challenge.getStatus() != Status.VALID) {
            throw new AcmeException("Failed to pass the challenge for domain "
                    + auth.getIdentifier().getDomain() + ", ... Giving up.");
        }
    }

    private Http01Challenge httpChallenge(Authorization auth) throws AcmeException {
        // Find a single http-01 challenge
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
        if (challenge == null) {
            throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
        }

        // Output the challenge, wait for acknowledge...
        log.debug("http://{}/.well-known/acme-challenge/{}", auth.getIdentifier().getDomain(), challenge.getToken());
        log.debug("Content: {}", challenge.getAuthorization());

        return challenge;
    }

}
