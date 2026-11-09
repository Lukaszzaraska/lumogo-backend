package pl.lumogo.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public TokenService(JwtEncoder encoder, @Value("${app.jwt.ttl-days}") long ttlDays) {
        this(encoder, Duration.ofDays(ttlDays), Clock.systemUTC());
    }

    TokenService(JwtEncoder encoder, Duration ttl, Clock clock) {
        this.encoder = encoder;
        this.ttl = ttl;
        this.clock = clock;
    }

    /** Token z adresem e-mail jako "sub". Bez odświeżania: po wygaśnięciu trzeba zalogować się ponownie. */
    public String issue(String email) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("lumogo")
            .subject(email)
            .issuedAt(now)
            .expiresAt(now.plus(ttl))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
