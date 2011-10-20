/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.lastfm.nonoauth.connect.web;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.web.ExtensibleProviderSignInController;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.lastfm.connect.LastFmConnectionFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Spring MVC Controller for handling the LastFm user sign-in flow. We cannot
 * use the existing ProviderSignInController, as LastFm's Auth scheme is neither
 * OAuth1 or OAuth2.
 * <ul>
 * <li>POST /signin/{providerId} - Initiate user sign-in with {providerId}.</li>
 * <li>GET /signin/{providerId}?oauth_token&oauth_verifier||code - Receive
 * {providerId} authentication callback and establish the connection.</li>
 * </ul>
 * 
 * @author Michael Lavelle
 */
public class LastFmNonOAuthSignInController extends ExtensibleProviderSignInController {

	private final LastFmNonOAuthConnectSupport lastFmWebSupport = new LastFmNonOAuthConnectSupport();

	@Inject
	public LastFmNonOAuthSignInController(
			ConnectionFactoryLocator connectionFactoryLocator,
			UsersConnectionRepository usersConnectionRepository,
			SignInAdapter signInAdapter) {
		super(connectionFactoryLocator, usersConnectionRepository,
				signInAdapter);
	}

	@RequestMapping(value = "/{providerId}", method = RequestMethod.GET, params = {
			"!code", "!oauth_token" })
	public View lastFmAuthCallback(@PathVariable String providerId,
			@RequestParam("token") String token, NativeWebRequest request) {
		LastFmConnectionFactory connectionFactory = (LastFmConnectionFactory) connectionFactoryLocator
				.getConnectionFactory(providerId);
		Connection<?> connection = lastFmWebSupport.completeConnection(
				connectionFactory, request);
		return handleSignIn(connection, request);
	}

	/**
	 * Configures the base secure URL for the application this controller is
	 * being used in e.g. <code>https://myapp.com</code>. Defaults to null. If
	 * specified, will be used to generate OAuth callback URLs. If not
	 * specified, OAuth callback URLs are generated from web request info. You
	 * may wish to set this property if requests into your application flow
	 * through a proxy to your application server. In this case, the request URI
	 * may contain a scheme, host, and/or port value that points to an internal
	 * server not appropriate for an external callback URL. If you have this
	 * problem, you can set this property to the base external URL for your
	 * application and it will be used to construct the callback URL instead.
	 * 
	 * @param applicationUrl
	 *            the application URL value
	 */
	public void setApplicationUrl(String applicationUrl) {
		try {
			lastFmWebSupport.setApplicationUrl(new URL(applicationUrl));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Process a sign-in form submission by commencing the process of
	 * establishing a connection to the provider on behalf of the user. For
	 * OAuth1, fetches a new request token from the provider, temporarily stores
	 * it in the session, then redirects the user to the provider's site for
	 * authentication authorization. For OAuth2, redirects the user to the
	 * provider's site for authentication authorization.
	 */
	@RequestMapping(value = "/{providerId}", method = RequestMethod.POST)
	public RedirectView signIn(@PathVariable String providerId,
			NativeWebRequest request) {

		ConnectionFactory<?> connectionFactory = connectionFactoryLocator
				.getConnectionFactory(providerId);
		if (connectionFactory instanceof LastFmConnectionFactory) {
			return new RedirectView(lastFmWebSupport.buildAuthUrl(
					(LastFmConnectionFactory) connectionFactory, request, null));
		} else {
			return super.signIn(providerId, request);

		}
	}

}
