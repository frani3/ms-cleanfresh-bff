package com.cleanfresh.ms_cleanfresh_bff.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Los access tokens de Cognito no tienen claim "aud" (a diferencia de
 * Azure), así que la propiedad "audiences" de Spring no sirve acá. Este
 * validador reemplaza esa verificación con tres chequeos equivalentes:
 *
 * - token_use == "access": para que nadie mande un idToken (pensado
 *   para el frontend, no para autorizar llamadas a este API) en vez del
 *   access token que corresponde.
 * - client_id == el de esta app: para que un token válido pero emitido
 *   para otra aplicación del mismo User Pool no sea aceptado acá.
 * - scope contiene el scope custom del Resource Server
 *   (https://api.cleanfresh.com/access_as_user): un access token válido
 *   pero sin este scope significa que el App Client no pidió (o no tiene
 *   autorizado) el permiso específico para hablarle a este API.
 */
public class CognitoTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedClientId;
    private final String requiredScope;

    public CognitoTokenValidator(String expectedClientId, String requiredScope) {
        this.expectedClientId = expectedClientId;
        this.requiredScope = requiredScope;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tokenUse = token.getClaimAsString("token_use");
        String clientId = token.getClaimAsString("client_id");
        String scope = token.getClaimAsString("scope");

        if (!"access".equals(tokenUse)) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "El token debe ser un access token (token_use=access).", null));
        }
        if (!expectedClientId.equals(clientId)) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "El token no fue emitido para esta aplicación.", null));
        }
        if (scope == null || !List.of(scope.split(" ")).contains(requiredScope)) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "insufficient_scope",
                    "El token no tiene el scope requerido: " + requiredScope,
                    null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
