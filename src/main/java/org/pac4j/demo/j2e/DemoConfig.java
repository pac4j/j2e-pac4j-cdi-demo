package org.pac4j.demo.j2e;


import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.client.CasProxyReceptor;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.authorization.authorizer.IsAnonymousAuthorizer;
import org.pac4j.core.authorization.authorizer.IsAuthenticatedAuthorizer;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.matching.PathMatcher;
import org.pac4j.demo.j2e.annotations.Initialized;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.ParameterClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.j2e.filter.CallbackFilter;
import org.pac4j.j2e.filter.LogoutFilter;
import org.pac4j.j2e.filter.SecurityFilter;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.config.signature.SignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.StravaClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


/**
 * Pac4J configuration used for demonstration and experimentation.
 *
 * NOTE: This might be better to do as a CDI producer, but it's not implemented that way yet.
 *
 * @author Phillip Ross
 */
@Named
@ApplicationScoped
public class DemoConfig {

    /** The static logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(DemoConfig.class);


    /**
     * Programmatically build a Pac4J configuration.
     *
     * @param servletContext the servlet context in which the configuration will apply
     * @return a Pac4j configuration object
     */
    public Config build(@Observes @Initialized ServletContext servletContext) {
        // First build all of the pac4j-specific configurations, clients, authorizers, etc.
        Config config = buildConfigurations();

        // Only filters for OIDC Google client are built for now.
        logger.debug("building servlet filters...");
        createAndRegisterGoogleOIDCFilter(servletContext, config);
        createAndRegisterCallbackFilter(servletContext, config);
        createAndRegisterLocalLogoutFilter(servletContext, config);
        return config;
    }


    /**
     * Programmatically build and register Pac4J google OIDC servlet filter.
     *
     * @param servletContext the servlet context in which the filter will reside
     */
    private void createAndRegisterGoogleOIDCFilter(final ServletContext servletContext, final Config config) {
        SecurityFilter securityFilter = new SecurityFilter();
        securityFilter.setConfig(config); // This populates the ConfigSingleton which has not been populated yet (true?)

        // Create, register, and map a filter which applies the Google OIDC client to the urls to be authorized by
        // the google OIDC mechanism.
        FilterRegistration.Dynamic oidcFilterRegistration = servletContext.addFilter(
                "OidcFilter",
                securityFilter
        );
        oidcFilterRegistration.setInitParameter("clients", "GoogleOidcClient");
        oidcFilterRegistration.setInitParameter("authorizers", "securityHeaders");
        oidcFilterRegistration.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                true,  // When this is true... declared mappings take precedence over this dynamic mapping
                "/oidc/*"
        );
    }


    /**
     * Programmatically build and register Pac4J callback servlet filter.
     *
     * @param servletContext the servlet context in which the filter will reside
     */
    private void createAndRegisterCallbackFilter(final ServletContext servletContext, final Config config) {
        CallbackFilter callbackFilter = new CallbackFilter();
        // The following will avoid RE-populating the ConfigSingleton which has already been populated when the
        // security filter config was set (true?)
        callbackFilter.setConfigOnly(config);
        FilterRegistration.Dynamic callbackFilterRegistration = servletContext.addFilter(
                "callbackFilter",
                callbackFilter
        );
        callbackFilterRegistration.setInitParameter("defaultUrl", "/");
        callbackFilterRegistration.setInitParameter("multiProfile", "true");
        callbackFilterRegistration.setInitParameter("renewSession", "true");
        callbackFilterRegistration.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                true,  // When this is true... declared mappings take precedence over this dynamic mapping
                "/callback"
        );
    }


    /**
     * Programmatically build and register Pac4J local logout servlet filter.
     *
     * @param servletContext the servlet context in which the filter will reside
     */
    private void createAndRegisterLocalLogoutFilter(final ServletContext servletContext, final Config config) {
        LogoutFilter logoutFilter = new LogoutFilter();
        logoutFilter.setConfigOnly(config);
        FilterRegistration.Dynamic localLogoutFilterRegistration = servletContext.addFilter(
                "logoutFilter",
                logoutFilter
        );
        localLogoutFilterRegistration.setInitParameter("defaultUrl", "/?defaulturlafterlogout");
        localLogoutFilterRegistration.setInitParameter("killSession", "true");
        localLogoutFilterRegistration.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                true,  // When this is true... declared mappings take precedence over this dynamic mapping
                "/logout"
        );
    }


    /**
     * Build the various Pac4J-specific configurations.
     *
     * @return a Pac4J config containing configurations, clients, authorizers, etc
     */
    private Config buildConfigurations() {
        logger.debug("building configurations...");

        // Google OIDC configuration/client
        final OidcConfiguration oidcConfiguration = new OidcConfiguration();
        oidcConfiguration.setClientId("167480702619-8e1lo80dnu8bpk3k0lvvj27noin97vu9.apps.googleusercontent.com");
        oidcConfiguration.setSecret("MhMme_Ik6IH2JMnAT6MFIfee");
        oidcConfiguration.setUseNonce(true);
        //oidcClient.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);
        oidcConfiguration.addCustomParam("prompt", "consent");
        final GoogleOidcClient oidcClient = new GoogleOidcClient(oidcConfiguration);
        oidcClient.setAuthorizationGenerator((ctx, profile) -> { profile.addRole("ROLE_ADMIN"); return profile; });

        final FormClient formClient = new FormClient(
                "http://localhost:8080/loginForm.jsp",
                new SimpleTestUsernamePasswordAuthenticator()
        );

        final SAML2ClientConfiguration cfg = new SAML2ClientConfiguration("resource:samlKeystore.jks",
                "pac4j-demo-passwd",
                "pac4j-demo-passwd",
                "resource:testshib-providers.xml");
        cfg.setMaximumAuthenticationLifetime(3600);
        cfg.setServiceProviderEntityId("http://localhost:8080/callback?client_name=SAML2Client");
        cfg.setServiceProviderMetadataPath(new File("sp-metadata.xml").getAbsolutePath());
        final SAML2Client saml2Client = new SAML2Client(cfg);

        final FacebookClient facebookClient = new FacebookClient("145278422258960", "be21409ba8f39b5dae2a7de525484da8");
        final TwitterClient twitterClient = new TwitterClient("CoxUiYwQOSFDReZYdjigBA", "2kAzunH5Btc4gRSaMr7D7MkyoJ5u1VzbOOzE8rBofs");
        // HTTP
        final IndirectBasicAuthClient indirectBasicAuthClient = new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        // CAS
        //final CasConfiguration configuration = new CasConfiguration("https://casserverpac4j.herokuapp.com/login");
        final CasConfiguration configuration = new CasConfiguration("http://localhost:8888/cas/login");
        final CasProxyReceptor casProxy = new CasProxyReceptor();
        configuration.setProxyReceptor(casProxy);
        final CasClient casClient = new CasClient(configuration);

        /*final DirectCasClient casClient = new DirectCasClient(configuration);
        casClient.setName("CasClient");*/

        // Strava
        final StravaClient stravaClient = new StravaClient();
        stravaClient.setApprovalPrompt("auto");
        // client_id
        stravaClient.setKey("3945");
        // client_secret
        stravaClient.setSecret("f03df80582396cddfbe0b895a726bac27c8cf739");
        stravaClient.setScope("view_private");

        // REST authent with JWT for a token passed in the url as the token parameter
        final List<SignatureConfiguration> signatures = new ArrayList<>();
        signatures.add(new SecretSignatureConfiguration(Constants.JWT_SALT));
        ParameterClient parameterClient = new ParameterClient("token", new JwtAuthenticator(signatures));
        parameterClient.setSupportGetRequest(true);
        parameterClient.setSupportPostRequest(false);

        // basic auth
        final DirectBasicAuthClient directBasicAuthClient = new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        final Clients clients = new Clients(
                "http://localhost:8080/callback",
                oidcClient,
                formClient,
                saml2Client, facebookClient, twitterClient, indirectBasicAuthClient, casClient, stravaClient,
                parameterClient, directBasicAuthClient, new AnonymousClient(), casProxy
        );

        final Config config = new Config(clients);
        config.addAuthorizer("admin", new RequireAnyRoleAuthorizer<>("ROLE_ADMIN"));
        config.addAuthorizer("custom", new CustomAuthorizer());
        config.addAuthorizer("mustBeAnon", new IsAnonymousAuthorizer<>("/?mustBeAnon"));
        config.addAuthorizer("mustBeAuth", new IsAuthenticatedAuthorizer<>("/?mustBeAuth"));
        config.addMatcher("excludedPath", new PathMatcher().excludeRegex("^/facebook/notprotected\\.jsp$"));
        return config;
    }


}
