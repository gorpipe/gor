package org.gorpipe.gor.auth.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.util.Strings;
import org.gorpipe.exceptions.GorSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class OAuthHandler {

    private RSAPublicKey rsaPublicKey;
    private JWTVerifier verifier;
    private final static Logger log = LoggerFactory.getLogger(OAuthHandler.class);

    public OAuthHandler(String publicKey) {
        this.rsaPublicKey = getRSAPublicKey(publicKey);
        this.verifier = getVerifier();
    }

    private RSAPublicKey getRSAPublicKey(String publicKey) {
        if (Strings.isNullOrEmpty(publicKey)) {
            log.error("ERROR: Public key is missing");
            throw new GorSystemException("ERROR: Public key is missing", null);
        }
        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
        RSAPublicKey rsaPublicKey;
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            rsaPublicKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("ERROR: Unable to construct RSA public key", e);
            throw new GorSystemException("ERROR: Unable to construct RSA public key", e);
        }
        return rsaPublicKey;
    }

    private JWTVerifier getVerifier() {
        Algorithm algorithm = Algorithm.RSA256(rsaPublicKey, null);
        return JWT.require(algorithm).build(); //Reusable verifier instance
    }

    public DecodedJWT decodeToken(String token) {
        return JWT.decode(token);
    }

    public DecodedJWT verifyAccessToken(String accessToken) {
        // By verifying the token, we also check for expiration and decode the token.
        return verifier.verify(accessToken);
    }
}