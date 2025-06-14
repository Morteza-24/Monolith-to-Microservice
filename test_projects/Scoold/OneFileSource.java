/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.App;
import com.erudika.para.core.annotations.Documented;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Para;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import static com.erudika.scoold.ScooldServer.SIGNOUTLINK;
import com.typesafe.config.ConfigObject;
import jakarta.inject.Named;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

/**
 * Scoold configuration.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@Named
public class ScooldConfig extends Config {

	@Override
	public com.typesafe.config.Config getFallbackConfig() {
		if (StringUtils.isBlank(System.getProperty("scoold.autoinit.para_config_file")) &&
				StringUtils.isBlank(System.getenv("scoold.autoinit.para_config_file"))) {
			return Para.getConfig().getConfig(); // fall back to para.* config
		}
		return com.typesafe.config.ConfigFactory.empty();
	}

	@Override
	public Set<String> getKeysExcludedFromRendering() {
		return Set.of("scoold", "security.ignored", "security.protected.admin");
	}

	@Override
	public String getConfigRootPrefix() {
		return "scoold";
	}

	/* **************************************************************************************************************
	 * Core                                                                                                    Core *
	 ****************************************************************************************************************/

	@Documented(position = 10,
			identifier = "app_name",
			value = "Scoold",
			category = "Core",
			description = "The formal name of the web application.")
	public String appName() {
		return getConfigParam("app_name", "Scoold");
	}

	@Documented(position = 20,
			identifier = "para_access_key",
			value = "app:scoold",
			category = "Core",
			tags = {"requires restart"},
			description = "App identifier (access key) of the Para app used by Scoold.")
	public String paraAccessKey() {
		return App.id(getConfigParam("para_access_key", getConfigParam("access_key", "app:scoold")));
	}

	@Documented(position = 30,
			identifier = "para_secret_key",
			value = "x",
			category = "Core",
			tags = {"requires restart"},
			description = "Secret key of the Para app used by Scoold.")
	public String paraSecretKey() {
		return getConfigParam("para_secret_key", getConfigParam("secret_key", "x"));
	}

	@Documented(position = 40,
			identifier = "para_endpoint",
			value = "http://localhost:8080",
			category = "Core",
			tags = {"requires restart"},
			description = "The URL of the Para server for Scoold to connects to. For hosted Para, use `https://paraio.com`")
	public String paraEndpoint() {
		return getConfigParam("para_endpoint", getConfigParam("endpoint", "http://localhost:8080"));
	}

	@Documented(position = 50,
			identifier = "host_url",
			value = "http://localhost:8000",
			category = "Core",
			description = "The internet-facing (public) URL of this Scoold server.")
	public String serverUrl() {
		return StringUtils.removeEnd(getConfigParam("host_url", "http://localhost:" + serverPort()), "/");
	}

	@Documented(position = 60,
			identifier = "port",
			value = "8000",
			type = Integer.class,
			category = "Core",
			tags = {"requires restart"},
			description = "The network port of this Scoold server. Port number should be a number above `1024`.")
	public int serverPort() {
		return NumberUtils.toInt(System.getProperty("server.port"), getConfigInt("port", 8000));
	}

	@Documented(position = 70,
			identifier = "env",
			value = "development",
			category = "Core",
			tags = {"requires restart"},
			description = "The environment profile to be used - possible values are `production` or `development`")
	public String environment() {
		return getConfigParam("env", "development");
	}

	@Documented(position = 80,
			identifier = "app_secret_key",
			category = "Core",
			description = "A random secret string, min. 32 chars long. *Must be different from the secret key of "
					+ "the Para app*. Used for generating JWTs and passwordless authentication tokens.")
	public String appSecretKey() {
		return getConfigParam("app_secret_key", "");
	}

	@Documented(position = 90,
			identifier = "admins",
			category = "Core",
			description = "A comma-separated list of emails of people who will be promoted to administrators with "
					+ "full rights over the content on the site. This can also contain Para user identifiers.")
	public String admins() {
		return getConfigParam("admins", "");
	}

	@Documented(position = 100,
			identifier = "is_default_space_public",
			value = "true",
			type = Boolean.class,
			category = "Core",
			description = "When enabled, all content in the default space will be publicly visible, "
					+ "without authentication, incl. users and tags. Disable to make the site private.")
	public boolean isDefaultSpacePublic() {
		return getConfigBoolean("is_default_space_public", true);
	}

	@Documented(position = 110,
			identifier = "context_path",
			category = "Core",
			tags = {"requires restart"},
			description = "The context path (subpath) of the web application, defaults to the root path `/`.")
	public String serverContextPath() {
		String context = getConfigParam("context_path", "");
		return StringUtils.stripEnd((StringUtils.isBlank(context) ?
				System.getProperty("server.servlet.context-path", "") : context), "/");
	}

	@Documented(position = 120,
			identifier = "webhooks_enabled",
			value = "true",
			type = Boolean.class,
			category = "Core",
			description = "Enable/disable webhooks support for events like `question.create`, `user.signup`, etc.")
	public boolean webhooksEnabled() {
		return getConfigBoolean("webhooks_enabled", true);
	}

	@Documented(position = 130,
			identifier = "api_enabled",
			value = "false",
			type = Boolean.class,
			category = "Core",
			description = "Enable/disable the Scoold RESTful API. Disabled by default.")
	public boolean apiEnabled() {
		return getConfigBoolean("api_enabled", false);
	}

	@Documented(position = 140,
			identifier = "feedback_enabled",
			value = "false",
			type = Boolean.class,
			category = "Core",
			description = "Enable/disable the feedback page on the site. It is intended for internal discussion "
					+ "about the website itself.")
	public boolean feedbackEnabled() {
		return getConfigBoolean("feedback_enabled", false);
	}


	/* **************************************************************************************************************
	 * Emails                                                                                                Emails *
	 ****************************************************************************************************************/

	@Documented(position = 150,
			identifier = "support_email",
			value = "contact@scoold.com",
			category = "Emails",
			description = "The email address to use for sending transactional emails, like welcome/password reset emails.")
	public String supportEmail() {
		return getConfigParam("support_email", "contact@scoold.com");
	}

	@Documented(position = 160,
			identifier = "mail.host",
			category = "Emails",
			description = "The SMTP server host to use for sending emails.")
	public String mailHost() {
		return getConfigParam("mail.host", "");
	}

	@Documented(position = 170,
			identifier = "mail.port",
			value = "587",
			type = Integer.class,
			category = "Emails",
			description = "The SMTP server port to use for sending emails.")
	public int mailPort() {
		return getConfigInt("mail.port", 587);
	}

	@Documented(position = 180,
			identifier = "mail.username",
			category = "Emails",
			description = "The SMTP server username.")
	public String mailUsername() {
		return getConfigParam("mail.username", "");
	}

	@Documented(position = 190,
			identifier = "mail.password",
			category = "Emails",
			description = "The SMTP server password.")
	public String mailPassword() {
		return getConfigParam("mail.password", "");
	}

	@Documented(position = 200,
			identifier = "mail.tls",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable TLS for the SMTP connection.")
	public boolean mailTLSEnabled() {
		return getConfigBoolean("mail.tls", true);
	}

	@Documented(position = 210,
			identifier = "mail.ssl",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable SSL for the SMTP connection.")
	public boolean mailSSLEnabled() {
		return getConfigBoolean("mail.ssl", false);
	}

	@Documented(position = 220,
			identifier = "mail.debug",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable debug information when sending emails through SMTP.")
	public boolean mailDebugEnabled() {
		return getConfigBoolean("mail.debug", false);
	}

	@Documented(position = 230,
			identifier = "favtags_emails_enabled",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Set the default toggle value for all users for receiving emails for new content "
					+ "with their favorite tags.")
	public boolean favoriteTagsEmailsEnabled() {
		return getConfigBoolean("favtags_emails_enabled", false);
	}

	@Documented(position = 240,
			identifier = "reply_emails_enabled",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Set the default toggle value for all users for receiving emails for answers to their questions.")
	public boolean replyEmailsEnabled() {
		return getConfigBoolean("reply_emails_enabled", false);
	}

	@Documented(position = 250,
			identifier = "comment_emails_enabled",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Set the default toggle value for all users for receiving emails for comments on their posts.")
	public boolean commentEmailsEnabled() {
		return getConfigBoolean("comment_emails_enabled", false);
	}

	@Documented(position = 260,
			identifier = "summary_email_period_days",
			value = "7",
			type = Integer.class,
			category = "Emails",
			tags = {"Pro"},
			description = "The time period between each content digest email, in days.")
	public int emailsSummaryIntervalDays() {
		return getConfigInt("summary_email_period_days", 7);
	}

	@Documented(position = 270,
			identifier = "summary_email_items",
			value = "25",
			type = Integer.class,
			category = "Emails",
			description = "The number of posts to include in the digest email (a summary of new posts).")
	public int emailsSummaryItems() {
		return getConfigInt("summary_email_items", 25);
	}

	@Documented(position = 280,
			identifier = "notification_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* notification emails.")
	public boolean notificationEmailsAllowed() {
		return getConfigBoolean("notification_emails_allowed", true);
	}

	@Documented(position = 290,
			identifier = "newpost_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* email notifications for every new question that is posted on the site.")
	public boolean emailsForNewPostsAllowed() {
		return getConfigBoolean("newpost_emails_allowed", true);
	}

	@Documented(position = 300,
			identifier = "favtags_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* email notifications for every new question tagged with a favorite tag.")
	public boolean emailsForFavtagsAllowed() {
		return getConfigBoolean("favtags_emails_allowed", true);
	}

	@Documented(position = 310,
			identifier = "reply_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* email notifications for every new answer that is posted on the site.")
	public boolean emailsForRepliesAllowed() {
		return getConfigBoolean("reply_emails_allowed", true);
	}

	@Documented(position = 320,
			identifier = "comment_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* email notifications for every new comment that is posted on the site.")
	public boolean emailsForCommentsAllowed() {
		return getConfigBoolean("comment_emails_allowed", true);
	}

	@Documented(position = 330,
			identifier = "mentions_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			tags = {"Pro"},
			description = "Enable/disable *all* email notifications every time a user is mentioned.")
	public boolean emailsForMentionsAllowed() {
		return getConfigBoolean("mentions_emails_allowed", true);
	}

	@Documented(position = 340,
			identifier = "summary_email_controlled_by_admins",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			tags = {"Pro"},
			description = "Controls whether admins can enable/disable summary emails for everyone from the 'Settings' page")
	public boolean emailsForSummaryControlledByAdmins() {
		return getConfigBoolean("summary_email_controlled_by_admins", false);
	}

	@Documented(position = 350,
			identifier = "mention_emails_controlled_by_admins",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			tags = {"Pro"},
			description = "Controls whether admins can enable/disable mention emails for everyone from the 'Settings' page")
	public boolean emailsForMentionsControlledByAdmins() {
		return getConfigBoolean("mention_emails_controlled_by_admins", false);
	}

	@Documented(position = 360,
			identifier = "emails.welcome_text1",
			value = "You are now part of {0} - a friendly Q&A community...",
			category = "Emails",
			description = "Allows for changing the default text (first paragraph) in the welcome email message.")
	public String emailsWelcomeText1(Map<String, String> lang) {
		return getConfigParam("emails.welcome_text1", lang.get("signin.welcome.body1") + "<br><br>");
	}

	@Documented(position = 370,
			identifier = "emails.welcome_text2",
			value = "To get started, simply navigate to the \"Ask question\" page and ask a question...",
			category = "Emails",
			description = "Allows for changing the default text (second paragraph) in the welcome email message.")
	public String emailsWelcomeText2(Map<String, String> lang) {
		return getConfigParam("emails.welcome_text2", lang.get("signin.welcome.body2") + "<br><br>");
	}

	@Documented(position = 380,
			identifier = "emails.welcome_text3",
			value = "Best, <br>The {0} team",
			category = "Emails",
			description = "Allows for changing the default text (signature at the end) in the welcome email message.")
	public String emailsWelcomeText3(Map<String, String> lang) {
		return getConfigParam("emails.welcome_text3", lang.get("notification.signature") + "<br><br>");
	}

	@Documented(position = 390,
			identifier = "emails.default_signature",
			value = "Best, <br>The {0} team",
			category = "Emails",
			description = "The default email signature for all transactional emails sent from Scoold.")
	public String emailsDefaultSignatureText(String defaultText) {
		return getConfigParam("emails.default_signature", defaultText);
	}

	/* **************************************************************************************************************
	 * Security                                                                                            Security *
	 ****************************************************************************************************************/

	@Documented(position = 400,
			identifier = "approved_domains_for_signups",
			category = "Security",
			description = "A comma-separated list of domain names, which will be used to restrict the people who "
					+ "are allowed to sign up on the site.")
	public String approvedDomainsForSignups() {
		return getConfigParam("approved_domains_for_signups", "");
	}

	@Documented(position = 410,
			identifier = "security.allow_unverified_emails",
			value = "false",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable email verification after the initial user registration. Users with unverified "
					+ "emails won't be able to sign in, unless they use a social login provider.")
	public boolean allowUnverifiedEmails() {
		return getConfigBoolean("security.allow_unverified_emails", StringUtils.isBlank(mailHost()));
	}

	@Documented(position = 420,
			identifier = "session_timeout",
			value = "86400",
			type = Integer.class,
			category = "Security",
			description = "The validity period of the authentication cookie, in seconds. Default is 24h.")
	public int sessionTimeoutSec() {
		return getConfigInt("session_timeout", Para.getConfig().sessionTimeoutSec());
	}

	@Documented(position = 430,
			identifier = "jwt_expires_after",
			value = "86400",
			type = Integer.class,
			category = "Security",
			description = "The validity period of the session token (JWT), in seconds. Default is 24h.")
	public int jwtExpiresAfterSec() {
		return getConfigInt("jwt_expires_after", Para.getConfig().jwtExpiresAfterSec());
	}

	@Documented(position = 440,
			identifier = "security.one_session_per_user",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "If disabled, users can sign in from multiple locations and devices, keeping a few open "
					+ "sessions at once. Otherwise, only one session will be kept open, others will be closed.")
	public boolean oneSessionPerUser() {
		return getConfigBoolean("security.one_session_per_user", true);
	}

	@Documented(position = 450,
			identifier = "min_password_length",
			value = "8",
			type = Integer.class,
			category = "Security",
			description = "The minimum length of passwords.")
	public int minPasswordLength() {
		return getConfigInt("min_password_length", Para.getConfig().minPasswordLength());
	}

	@Documented(position = 460,
			identifier = "min_password_strength",
			value = "2",
			type = Integer.class,
			category = "Security",
			description = "The minimum password strength - one of 3 levels: `1` good enough, `2` strong, `3` very strong.")
	public int minPasswordStrength() {
		return getConfigInt("min_password_strength", 2);
	}

	@Documented(position = 470,
			identifier = "pass_reset_timeout",
			value = "1800",
			type = Integer.class,
			category = "Security",
			description = "The validity period of the password reset token sent via email for resetting users' "
					+ "passwords. Default is 30 min.")
	public int passwordResetTimeoutSec() {
		return getConfigInt("pass_reset_timeout", Para.getConfig().passwordResetTimeoutSec());
	}

	@Documented(position = 480,
			identifier = "profile_anonimity_enabled",
			value = "false",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the option for users to anonimize their profiles on the site, "
					+ "hiding their name and picture.")
	public boolean profileAnonimityEnabled() {
		return getConfigBoolean("profile_anonimity_enabled", false);
	}

	@Documented(position = 490,
			identifier = "signup_captcha_site_key",
			category = "Security",
			description = "The reCAPTCHA v3 site key for protecting the signup and password reset pages.")
	public String captchaSiteKey() {
		return getConfigParam("signup_captcha_site_key", "");
	}

	@Documented(position = 500,
			identifier = "signup_captcha_secret_key",
			category = "Security",
			description = "The reCAPTCHA v3 secret.")
	public String captchaSecretKey() {
		return getConfigParam("signup_captcha_secret_key", "");
	}
	@Documented(position = 510,
			identifier = "csp_reports_enabled",
			value = "false",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable automatic reports each time the Content Security Policy is violated.")
	public boolean cspReportsEnabled() {
		return getConfigBoolean("csp_reports_enabled", false);
	}

	@Documented(position = 520,
			identifier = "csp_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the Content Security Policy (CSP) header.")
	public boolean cspHeaderEnabled() {
		return getConfigBoolean("csp_header_enabled", true);
	}

	@Documented(position = 530,
			identifier = "csp_header",
			category = "Security",
			description = "The CSP header value which will overwrite the default one. This can contain one or more "
					+ "`{{nonce}}` placeholders, which will be replaced with an actual nonce on each request.")
	public String cspHeader(String nonce) {
		return getConfigParam("csp_header", getDefaultContentSecurityPolicy()).replaceAll("\\{\\{nonce\\}\\}", nonce);
	}

	@Documented(position = 540,
			identifier = "hsts_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `Strict-Transport-Security` security header.")
	public boolean hstsHeaderEnabled() {
		return getConfigBoolean("hsts_header_enabled", true);
	}

	@Documented(position = 550,
			identifier = "framing_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `X-Frame-Options` security header.")
	public boolean framingHeaderEnabled() {
		return getConfigBoolean("framing_header_enabled", true);
	}

	@Documented(position = 560,
			identifier = "xss_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `X-XSS-Protection` security header.")
	public boolean xssHeaderEnabled() {
		return getConfigBoolean("xss_header_enabled", true);
	}

	@Documented(position = 570,
			identifier = "contenttype_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `X-Content-Type-Options` security header.")
	public boolean contentTypeHeaderEnabled() {
		return getConfigBoolean("contenttype_header_enabled", true);
	}

	@Documented(position = 580,
			identifier = "referrer_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `Referrer-Policy` security header.")
	public boolean referrerHeaderEnabled() {
		return getConfigBoolean("referrer_header_enabled", true);
	}

	@Documented(position = 590,
			identifier = "permissions_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `Permissions-Policy` security header.")
	public boolean permissionsHeaderEnabled() {
		return getConfigBoolean("permissions_header_enabled", true);
	}

	@Documented(position = 600,
			identifier = "csp_connect_sources",
			category = "Security",
			description = "Additional sources to add to the `connect-src` CSP directive. "
					+ "Used when adding external scripts to the site.")
	public String cspConnectSources() {
		return getConfigParam("csp_connect_sources", "");
	}

	@Documented(position = 610,
			identifier = "csp_frame_sources",
			category = "Security",
			description = "Additional sources to add to the `frame-src` CSP directive. "
					+ "Used when adding external scripts to the site.")
	public String cspFrameSources() {
		return getConfigParam("csp_frame_sources", "");
	}

	@Documented(position = 620,
			identifier = "csp_font_sources",
			category = "Security",
			description = "Additional sources to add to the `font-src` CSP directive. "
					+ "Used when adding external fonts to the site.")
	public String cspFontSources() {
		return getConfigParam("csp_font_sources", "");
	}

	@Documented(position = 630,
			identifier = "csp_style_sources",
			value = "",
			category = "Security",
			description = "Additional sources to add to the `style-src` CSP directive. "
					+ "Used when adding external fonts to the site.")
	public String cspStyleSources() {
		return getConfigParam("csp_style_sources", serverUrl() + stylesheetUrl() + " " +
				externalStyles().replaceAll(",", ""));
	}

	/* **************************************************************************************************************
	 * Basic Authentication                                                                    Basic Authentication *
	 ****************************************************************************************************************/

	@Documented(position = 640,
			identifier = "password_auth_enabled",
			value = "true",
			type = Boolean.class,
			category = "Basic Authentication",
			description = "Enabled/disable the ability for users to sign in with an email and password.")
	public boolean passwordAuthEnabled() {
		return getConfigBoolean("password_auth_enabled", true);
	}

	@Documented(position = 650,
			identifier = "fb_app_id",
			category = "Basic Authentication",
			description = "Facebook OAuth2 app ID.")
	public String facebookAppId() {
		return getConfigParam("fb_app_id", "");
	}

	@Documented(position = 660,
			identifier = "fb_secret",
			category = "Basic Authentication",
			description = "Facebook app secret key.")
	public String facebookSecret() {
		return getConfigParam("fb_secret", "");
	}

	@Documented(position = 670,
			identifier = "gp_app_id",
			category = "Basic Authentication",
			description = "Google OAuth2 app ID.")
	public String googleAppId() {
		return getConfigParam("gp_app_id", "");
	}

	@Documented(position = 680,
			identifier = "gp_secret",
			category = "Basic Authentication",
			description = "Google app secret key.")
	public String googleSecret() {
		return getConfigParam("gp_secret", "");
	}

	@Documented(position = 690,
			identifier = "in_app_id",
			category = "Basic Authentication",
			description = "LinkedIn OAuth2 app ID.")
	public String linkedinAppId() {
		return getConfigParam("in_app_id", "");
	}

	@Documented(position = 700,
			identifier = "in_secret",
			category = "Basic Authentication",
			description = "LinkedIn app secret key.")
	public String linkedinSecret() {
		return getConfigParam("in_secret", "");
	}

	@Documented(position = 710,
			identifier = "tw_app_id",
			category = "Basic Authentication",
			description = "Twitter OAuth app ID.")
	public String twitterAppId() {
		return getConfigParam("tw_app_id", "");
	}

	@Documented(position = 720,
			identifier = "tw_secret",
			category = "Basic Authentication",
			description = "Twitter app secret key.")
	public String twitterSecret() {
		return getConfigParam("tw_secret", "");
	}

	@Documented(position = 730,
			identifier = "gh_app_id",
			category = "Basic Authentication",
			description = "GitHub OAuth2 app ID.")
	public String githubAppId() {
		return getConfigParam("gh_app_id", "");
	}

	@Documented(position = 740,
			identifier = "gh_secret",
			category = "Basic Authentication",
			description = "GitHub app secret key.")
	public String githubSecret() {
		return getConfigParam("gh_secret", "");
	}

	@Documented(position = 750,
			identifier = "ms_app_id",
			category = "Basic Authentication",
			description = "Microsoft OAuth2 app ID.")
	public String microsoftAppId() {
		return getConfigParam("ms_app_id", "");
	}

	@Documented(position = 760,
			identifier = "ms_secret",
			category = "Basic Authentication",
			description = "Microsoft app secret key.")
	public String microsoftSecret() {
		return getConfigParam("ms_secret", "");
	}

	@Documented(position = 770,
			identifier = "ms_tenant_id",
			value = "common",
			category = "Basic Authentication",
			description = "Microsoft OAuth2 tenant ID")
	public String microsoftTenantId() {
		return getConfigParam("ms_tenant_id", "common");
	}

	@Documented(position = 780,
			identifier = "az_app_id",
			category = "Basic Authentication",
			description = "Amazon OAuth2 app ID.")
	public String amazonAppId() {
		return getConfigParam("az_app_id", "");
	}

	@Documented(position = 790,
			identifier = "az_secret",
			category = "Basic Authentication",
			description = "Amazon app secret key.")
	public String amazonSecret() {
		return getConfigParam("az_secret", "");
	}

	@Documented(position = 800,
			identifier = "sl_app_id",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "Slack OAuth2 app ID.")
	public String slackAppId() {
		return getConfigParam("sl_app_id", "");
	}

	@Documented(position = 810,
			identifier = "sl_secret",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "Slack app secret key.")
	public String slackSecret() {
		return getConfigParam("sl_secret", "");
	}

	@Documented(position = 820,
			identifier = "mm_app_id",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "Mattermost OAuth2 app ID.")
	public String mattermostAppId() {
		return getConfigParam("mm_app_id", "");
	}

	@Documented(position = 830,
			identifier = "mm_secret",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "Mattermost app secret key.")
	public String mattermostSecret() {
		return getConfigParam("mm_secret", "");
	}

	@Documented(position = 840,
			identifier = "security.custom.provider",
			value = "Continue with Acme Co.",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "The text on the button for signing in with the custom authentication scheme.")
	public String customLoginProvider() {
		return getConfigParam("security.custom.provider", "Continue with Acme Co.");
	}

	@Documented(position = 850,
			identifier = "security.custom.login_url",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "The URL address of an externally hosted, custom login page.")
	public String customLoginUrl() {
		return getConfigParam("security.custom.login_url", "");
	}

	/* **************************************************************************************************************
	 * LDAP Authentication                                                                      LDAP Authentication *
	 ****************************************************************************************************************/

	@Documented(position = 860,
			identifier = "security.ldap.server_url",
			category = "LDAP Authentication",
			description = "LDAP server URL. LDAP will be disabled if this is blank.")
	public String ldapServerUrl() {
		return getConfigParam("security.ldap.server_url", "");
	}

	@Documented(position = 870,
			identifier = "security.ldap.base_dn",
			category = "LDAP Authentication",
			description = "LDAP base DN.")
	public String ldapBaseDN() {
		return getConfigParam("security.ldap.base_dn", "");
	}

	@Documented(position = 880,
			identifier = "security.ldap.user_search_base",
			category = "LDAP Authentication",
			description = "LDAP search base, which will be used only if a direct bind is unsuccessfull.")
	public String ldapUserSearchBase() {
		return getConfigParam("security.ldap.user_search_base", "");
	}

	@Documented(position = 890,
			identifier = "security.ldap.user_search_filter",
			value = "(cn={0})",
			category = "LDAP Authentication",
			description = "LDAP search filter, for finding users if a direct bind is unsuccessful.")
	public String ldapUserSearchFilter() {
		return getConfigParam("security.ldap.user_search_filter", "(cn={0})");
	}

	@Documented(position = 900,
			identifier = "security.ldap.user_dn_pattern",
			value = "uid={0}",
			category = "LDAP Authentication",
			description = "LDAP user DN pattern, which will be comined with the base DN to form the full path to the"
					+ "user object, for a direct binding attempt.")
	public String ldapUserDNPattern() {
		return getConfigParam("security.ldap.user_dn_pattern", "uid={0}");
	}

	@Documented(position = 901,
			identifier = "security.ldap.ad_mode_enabled",
			value = "false",
			type = Boolean.class,
			category = "LDAP Authentication",
			description = "Enable/disable support for authenticating with Active Directory. If `true`, AD is enabled.")
	public Boolean ldapActiveDirectoryEnabled() {
		return getConfigBoolean("security.ldap.ad_mode_enabled", false);
	}

	@Documented(position = 910,
			identifier = "security.ldap.active_directory_domain",
			category = "LDAP Authentication",
			description = "AD domain name. Add this *only* if you are connecting to an Active Directory server.")
	public String ldapActiveDirectoryDomain() {
		return getConfigParam("security.ldap.active_directory_domain", "");
	}

	@Documented(position = 920,
			identifier = "security.ldap.password_attribute",
			value = "userPassword",
			category = "LDAP Authentication",
			description = "LDAP password attribute name.")
	public String ldapPasswordAttributeName() {
		return getConfigParam("security.ldap.password_attribute", "userPassword");
	}

	@Documented(position = 930,
			identifier = "security.ldap.bind_dn",
			category = "LDAP Authentication",
			description = "LDAP bind DN")
	public String ldapBindDN() {
		return getConfigParam("security.ldap.bind_dn", "");
	}

	@Documented(position = 940,
			identifier = "security.ldap.bind_pass",
			category = "LDAP Authentication",
			description = "LDAP bind password.")
	public String ldapBindPassword() {
		return getConfigParam("security.ldap.bind_pass", "");
	}

	@Documented(position = 950,
			identifier = "security.ldap.username_as_name",
			value = "false",
			type = Boolean.class,
			category = "LDAP Authentication",
			description = "Enable/disable the use of usernames for names on Scoold.")
	public boolean ldapUsernameAsName() {
		return getConfigBoolean("security.ldap.username_as_name", false);
	}

	@Documented(position = 960,
			identifier = "security.ldap.provider",
			value = "Continue with LDAP",
			category = "LDAP Authentication",
			tags = {"Pro"},
			description = "The text on the LDAP sign in button.")
	public String ldapProvider() {
		return getConfigParam("security.ldap.provider", "Continue with LDAP");
	}

	@Documented(position = 970,
			identifier = "security.ldap.mods_group_node",
			category = "LDAP Authentication",
			description = "Moderators group mapping, mapping LDAP users with this node, to moderators on Scoold.")
	public String ldapModeratorsGroupNode() {
		return getConfigParam("security.ldap.mods_group_node", "");
	}

	@Documented(position = 980,
			identifier = "security.ldap.admins_group_node",
			category = "LDAP Authentication",
			description = "Administrators group mapping, mapping LDAP users with this node, to administrators on Scoold.")
	public String ldapAdministratorsGroupNode() {
		return getConfigParam("security.ldap.admins_group_node", "");
	}

	@Documented(position = 990,
			identifier = "security.ldap.compare_passwords",
			category = "LDAP Authentication",
			description = "LDAP compare passwords.")
	public String ldapComparePasswords() {
		return getConfigParam("security.ldap.compare_passwords", "");
	}

	@Documented(position = 1000,
			identifier = "security.ldap.password_param",
			value = "password",
			category = "LDAP Authentication",
			description = "LDAP password parameter name.")
	public String ldapPasswordParameter() {
		return getConfigParam("security.ldap.password_param", "password");
	}

	@Documented(position = 1010,
			identifier = "security.ldap.username_param",
			value = "username",
			category = "LDAP Authentication",
			description = "LDAP username parameter name.")
	public String ldapUsernameParameter() {
		return getConfigParam("security.ldap.username_param", "username");
	}

	@Documented(position = 1020,
			identifier = "security.ldap.is_local",
			value = "false",
			type = Boolean.class,
			category = "LDAP Authentication",
			tags = {"Pro"},
			description = "Enable/disable local handling of LDAP requests, instead of sending those to Para.")
	public boolean ldapIsLocal() {
		return getConfigBoolean("security.ldap.is_local", false);
	}

	/* **************************************************************************************************************
	 * SAML Authentication                                                                      SAML Authentication *
	 ****************************************************************************************************************/

	@Documented(position = 1030,
			identifier = "security.saml.idp.metadata_url",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML metadata URL. Scoold will fetch most of the necessary information for the authentication"
					+ " request from that XML document. This will overwrite all other IDP settings.")
	public String samlIDPMetadataUrl() {
		return getConfigParam("security.saml.idp.metadata_url", "");
	}

	@Documented(position = 1040,
			identifier = "security.saml.sp.entityid",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML SP endpoint address - e.g. `https://paraio.com/saml_auth/scoold`. The IDP will call "
					+ "this address for authentication.")
	public String samlSPEntityId() {
		if (samlIsLocal()) {
			return serverUrl() + serverContextPath() + "/saml_auth";
		}
		return getConfigParam("security.saml.sp.entityid", "");
	}

	@Documented(position = 1050,
			identifier = "security.saml.sp.x509cert",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML client x509 certificate for the SP (public key). **Value must be Base64-encoded**.")
	public String samlSPX509Certificate() {
		return getConfigParam("security.saml.sp.x509cert", "");
	}

	@Documented(position = 1060,
			identifier = "security.saml.sp.privatekey",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML client private key in PKCS#8 format for the SP. **Value must be Base64-encoded**.")
	public String samlSPX509PrivateKey() {
		return getConfigParam("security.saml.sp.privatekey", "");
	}

	@Documented(position = 1070,
			identifier = "security.saml.attributes.id",
			value = "UserID",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `id`.")
	public String samlIdAttribute() {
		return getConfigParam("security.saml.attributes.id", "UserID");
	}

	@Documented(position = 1080,
			identifier = "security.saml.attributes.picture",
			value = "Picture",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `picture`.")
	public String samlPictureAttribute() {
		return getConfigParam("security.saml.attributes.picture", "Picture");
	}

	@Documented(position = 1090,
			identifier = "security.saml.attributes.email",
			value = "EmailAddress",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `email`.")
	public String samlEmailAttribute() {
		return getConfigParam("security.saml.attributes.email", "EmailAddress");
	}

	@Documented(position = 1100,
			identifier = "security.saml.attributes.name",
			value = "GivenName",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `name`.")
	public String samlNameAttribute() {
		return getConfigParam("security.saml.attributes.name", "GivenName");
	}

	@Documented(position = 1110,
			identifier = "security.saml.attributes.firstname",
			value = "FirstName",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `firstname`.")
	public String samlFirstNameAttribute() {
		return getConfigParam("security.saml.attributes.firstname", "FirstName");
	}

	@Documented(position = 1120,
			identifier = "security.saml.attributes.lastname",
			value = "LastName",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `lastname`.")
	public String samlLastNameAttribute() {
		return getConfigParam("security.saml.attributes.lastname", "LastName");
	}

	@Documented(position = 1130,
			identifier = "security.saml.provider",
			value = "Continue with SAML",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "The text on the button for signing in with SAML.")
	public String samlProvider() {
		return getConfigParam("security.saml.provider", "Continue with SAML");
	}

	@Documented(position = 1140,
			identifier = "security.saml.sp.assertion_consumer_service.url",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML ACS URL.")
	public String samlSPAssertionConsumerServiceUrl() {
		return getConfigParam("security.saml.sp.assertion_consumer_service.url", samlIsLocal() ? samlSPEntityId() : "");
	}

	@Documented(position = 1150,
			identifier = "security.saml.sp.nameidformat",
			value = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML name id format.")
	public String samlSPNameIdFormat() {
		return getConfigParam("security.saml.sp.nameidformat", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
	}

	@Documented(position = 1160,
			identifier = "security.saml.idp.entityid",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML IDP entity id for manually setting the endpoint address of the IDP, instead of getting "
					+ "it from the provided metadata URL.")
	public String samlIDPEntityId() {
		return getConfigParam("security.saml.idp.entityid", "");
	}

	@Documented(position = 1170,
			identifier = "security.saml.idp.single_sign_on_service.url",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML SSO service URL of the IDP.")
	public String samlIDPSingleSignOnServiceUrl() {
		return getConfigParam("security.saml.idp.single_sign_on_service.url", "");
	}

	@Documented(position = 1180,
			identifier = "security.saml.idp.x509cert",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML server x509 certificate for the IDP (public key). **Value must be Base64-encoded**.")
	public String samlIDPX509Certificate() {
		return getConfigParam("security.saml.idp.x509cert", "");
	}

	@Documented(position = 1190,
			identifier = "security.saml.security.authnrequest_signed",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML authentication request signing.")
	public boolean samlAuthnRequestSigningEnabled() {
		return getConfigBoolean("security.saml.security.authnrequest_signed", false);
	}

	@Documented(position = 1200,
			identifier = "security.saml.security.want_messages_signed",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML message signing.")
	public boolean samlMessageSigningEnabled() {
		return getConfigBoolean("security.saml.security.want_messages_signed", false);
	}

	@Documented(position = 1210,
			identifier = "security.saml.security.want_assertions_signed",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML assertion signing.")
	public boolean samlAssertionSigningEnabled() {
		return getConfigBoolean("security.saml.security.want_assertions_signed", false);
	}

	@Documented(position = 1220,
			identifier = "security.saml.security.want_assertions_encrypted",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML assertion encryption.")
	public boolean samlAssertionEncryptionEnabled() {
		return getConfigBoolean("security.saml.security.want_assertions_encrypted", false);
	}

	@Documented(position = 1230,
			identifier = "security.saml.security.want_nameid_encrypted",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML NameID encryption.")
	public boolean samlNameidEncryptionEnabled() {
		return getConfigBoolean("security.saml.security.want_nameid_encrypted", false);
	}

	@Documented(position = 1231,
			identifier = "security.saml.security.want_nameid",
			value = "true",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML NameID requirement.")
	public boolean samlNameidEnabled() {
		return getConfigBoolean("security.saml.security.want_nameid", true);
	}

	@Documented(position = 1240,
			identifier = "security.saml.security.sign_metadata",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML metadata signing.")
	public boolean samlMetadataSigningEnabled() {
		return getConfigBoolean("security.saml.security.sign_metadata", false);
	}

	@Documented(position = 1250,
			identifier = "security.saml.security.want_xml_validation",
			value = "true",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML XML validation.")
	public boolean samlXMLValidationEnabled() {
		return getConfigBoolean("security.saml.security.want_xml_validation", true);
	}

	@Documented(position = 1260,
			identifier = "security.saml.security.signature_algorithm",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML signature algorithm.")
	public String samlSignatureAlgorithm() {
		return getConfigParam("security.saml.security.signature_algorithm", "");
	}

	@Documented(position = 1270,
			identifier = "security.saml.domain",
			value = "paraio.com",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML domain name.")
	public String samlDomain() {
		return getConfigParam("security.saml.domain", "paraio.com");
	}

	@Documented(position = 1280,
			identifier = "security.saml.is_local",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable local handling of SAML requests, instead of sending those to Para.")
	public boolean samlIsLocal() {
		return getConfigBoolean("security.saml.is_local", false);
	}

	/* **************************************************************************************************************
	 * OAuth 2.0 authentication                                                            OAuth 2.0 authentication *
	 ****************************************************************************************************************/

	@Documented(position = 1290,
			identifier = "oa2_app_id",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app identifier. Alternatives: `oa2second_app_id`, `oa2third_app_id`")
	public String oauthAppId(String a) {
		return getConfigParam("oa2" + a + "_app_id", "");
	}

	@Documented(position = 1300,
			identifier = "oa2_secret",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app secret key. Alternatives: `oa2second_secret`, `oa2third_secret`")
	public String oauthSecret(String a) {
		return getConfigParam("oa2" + a + "_secret", "");
	}

	@Documented(position = 1310,
			identifier = "security.oauth.authz_url",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app authorization URL (login page). Alternatives: "
					+ "`security.oauthsecond.authz_url`, `security.oauththird.authz_url`")
	public String oauthAuthorizationUrl(String a) {
		return getConfigParam("security.oauth" + a + ".authz_url", "");
	}

	@Documented(position = 1320,
			identifier = "security.oauth.token_url",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app token endpoint URL. Alternatives: `security.oauthsecond.token_url`, "
					+ "`security.oauththird.token_url`")
	public String oauthTokenUrl(String a) {
		return getConfigParam("security.oauth" + a + ".token_url", "");
	}

	@Documented(position = 1330,
			identifier = "security.oauth.profile_url",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app user info endpoint URL. Alternatives: `security.oauthsecond.profile_url`, "
					+ "`security.oauththird.profile_url`")
	public String oauthProfileUrl(String a) {
		return getConfigParam("security.oauth" + a + ".profile_url", "");
	}

	@Documented(position = 1340,
			identifier = "security.oauth.scope",
			value = "openid email profile",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app scope. Alternatives: `security.oauthsecond.scope`, "
					+ "`security.oauththird.scope`")
	public String oauthScope(String a) {
		return getConfigParam("security.oauth" + a + ".scope", "openid email profile");
	}

	@Documented(position = 1350,
			identifier = "security.oauth.accept_header",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 `Accept` header customization. Alternatives: `security.oauthsecond.accept_header`, "
					+ "`security.oauththird.accept_header`")
	public String oauthAcceptHeader(String a) {
		return getConfigParam("security.oauth" + a + ".accept_header", "");
	}

	@Documented(position = 1360,
			identifier = "security.oauth.parameters.id",
			value = "sub",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `id`. Alternatives: `security.oauthsecond.parameters.id`, "
					+ "`security.oauththird.parameters.id`")
	public String oauthIdParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.id", null);
	}

	@Documented(position = 1370,
			identifier = "security.oauth.parameters.name",
			value = "name",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `name`. Alternatives: `security.oauthsecond.parameters.name`, "
					+ "`security.oauththird.parameters.name`")
	public String oauthNameParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.name", null);
	}

	@Documented(position = 1380,
			identifier = "security.oauth.parameters.given_name",
			value = "given_name",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `given_name`. Alternatives: "
					+ "`security.oauthsecond.parameters.given_name`, `security.oauththird.parameters.given_name`")
	public String oauthGivenNameParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.given_name", null);
	}

	@Documented(position = 1390,
			identifier = "security.oauth.parameters.family_name",
			value = "family_name",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `family_name`. Alternatives: "
					+ "`security.oauthsecond.parameters.family_name`, `security.oauththird.parameters.family_name`")
	public String oauthFamiliNameParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.family_name", null);
	}

	@Documented(position = 1400,
			identifier = "security.oauth.parameters.email",
			value = "email",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `email`. Alternatives: `security.oauthsecond.parameters.email`, "
					+ "`security.oauththird.parameters.email`")
	public String oauthEmailParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.email", null);
	}

	@Documented(position = 1410,
			identifier = "security.oauth.parameters.picture",
			value = "picture",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `picture`. Alternatives: `security.oauthsecond.parameters.picture`, "
					+ "`security.oauththird.parameters.picture`")
	public String oauthPictureParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.picture", null);
	}

	@Documented(position = 1420,
			identifier = "security.oauth.download_avatars",
			value = "false",
			type = Boolean.class,
			category = "OAuth 2.0 Authentication",
			description = "Enable/disable OAauth 2.0 avatar downloading to local disk. Used when avatars are large in size. "
					+ "Alternatives: `security.oauthsecond.download_avatars`, `security.oauththird.download_avatars`")
	public boolean oauthAvatarDownloadingEnabled(String a) {
		return getConfigBoolean("security.oauth" + a + ".download_avatars", false);
	}

	@Documented(position = 1430,
			identifier = "security.oauth.token_delegation_enabled",
			value = "false",
			type = Boolean.class,
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "Enable/disable OAauth 2.0 token delegation. The ID and access tokens will be saved and "
					+ "delegated to Scoold from Para. Alternatives: `security.oauthsecond.token_delegation_enabled`, "
					+ "`security.oauththird.token_delegation_enabled`")
	public boolean oauthTokenDelegationEnabled(String a) {
		return getConfigBoolean("security.oauth" + a + ".token_delegation_enabled", false);
	}

	@Documented(position = 1440,
			identifier = "security.oauth.spaces_attribute_name",
			value = "spaces",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 attribute mapping for users' `spaces`. The spaces can be comma-separated. "
					+ "Alternatives: `security.oauthsecond.spaces_attribute_name`, "
					+ "`security.oauththird.spaces_attribute_name`")
	public String oauthSpacesAttributeName(String a) {
		return getConfigParam("security.oauth" + a + ".spaces_attribute_name", "spaces");
	}

	@Documented(position = 1450,
			identifier = "security.oauth.groups_attribute_name",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 attribute mapping for users' `groups`. "
					+ "Use this for mapping `admin`, `mod` and `user` roles to Scoold users."
					+ "Alternatives: `security.oauthsecond.groups_attribute_name`, "
					+ "`security.oauththird.groups_attribute_name`")
	public String oauthGroupsAttributeName(String a) {
		return getConfigParam("security.oauth" + a + ".groups_attribute_name", "");
	}

	@Documented(position = 1460,
			identifier = "security.oauth.mods_equivalent_claim_value",
			value = "mod",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 claim used for mapping OAuth2 users having it, "
					+ "to moderators on Scoold. Alternatives: `security.oauthsecond.mods_equivalent_claim_value`, "
					+ "`security.oauththird.mods_equivalent_claim_value`")
	public String oauthModeratorsEquivalentClaim(String a) {
		return getConfigParam("security.oauth" + a + ".mods_equivalent_claim_value", "mod");
	}

	@Documented(position = 1470,
			identifier = "security.oauth.admins_equivalent_claim_value",
			value = "admin",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 claim used for mapping OAuth2 users having it, "
					+ "to administrators on Scoold. Alternatives: `security.oauthsecond.admins_equivalent_claim_value`, "
					+ "`security.oauththird.admins_equivalent_claim_value`")
	public String oauthAdministratorsEquivalentClaim(String a) {
		return getConfigParam("security.oauth" + a + ".admins_equivalent_claim_value", "admin");
	}

	@Documented(position = 1480,
			identifier = "security.oauth.users_equivalent_claim_value",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 claim used for **denying access** to OAuth2 users **not** having it, *unless*"
					+ "they already have the admin or moderator roles assigned. "
					+ "Alternatives: `security.oauthsecond.users_equivalent_claim_value`, "
					+ "`security.oauththird.users_equivalent_claim_value`")
	public String oauthUsersEquivalentClaim(String a) {
		return getConfigParam("security.oauth" + a + ".users_equivalent_claim_value", "");
	}

	@Documented(position = 1490,
			identifier = "security.oauth.domain",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 domain name for constructing user email addresses in case they are missing. "
					+ "Alternatives: `security.oauthsecond.domain`, `security.oauththird.domain`")
	public String oauthDomain(String a) {
		return getConfigParam("security.oauth" + a + ".domain", null);
	}

	@Documented(position = 1500,
			identifier = "security.oauth.provider",
			value = "Continue with OpenID Connect",
			category = "OAuth 2.0 Authentication",
			description = "The text on the button for signing in with OAuth2 or OIDC.")
	public String oauthProvider(String a) {
		return getConfigParam("security.oauth" + a + ".provider", "Continue with " + a + "OpenID Connect");
	}

	@Documented(position = 1501,
			identifier = "security.oauth.appid_in_state_param_enabled",
			value = "true",
			type = Boolean.class,
			category = "OAuth 2.0 Authentication",
			description = "Enable/disable the use of the OAauth 2.0 state parameter to designate your Para app id. "
					+ "Some OAauth 2.0 servers throw errors if the length of the state parameter is less than 8 chars.")
	public boolean oauthAppidInStateParamEnabled(String a) {
		return getConfigBoolean("security.oauth" + a + ".appid_in_state_param_enabled", true);
	}

	@Documented(position = 1502,
			identifier = "security.oauth.send_scope_to_token_endpoint",
			value = "true",
			type = Boolean.class,
			category = "OAuth 2.0 Authentication",
			description = "Enable/disable sending the OAauth 2.0 scope parameter in the token request. "
					+ "Some OAuth 2.0 servers require this to be turned off.")
	public boolean oauthSendScopeToTokenEndpointEnabled(String a) {
		return getConfigBoolean("security.oauth" + a + ".send_scope_to_token_endpoint", true);
	}

	/* **************************************************************************************************************
	 * Posts                                                                                                  Posts *
	 ****************************************************************************************************************/

	@Documented(position = 1510,
			identifier = "new_users_can_comment",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the ability for users with reputation below 100 to comments on posts.")
	public boolean newUsersCanComment() {
		return getConfigBoolean("new_users_can_comment", true);
	}

	@Documented(position = 1520,
			identifier = "posts_need_approval",
			value = "false",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the need for approval of new posts (both questions and answers) by a moderator. ")
	public boolean postsNeedApproval() {
		return getConfigBoolean("posts_need_approval", false);
	}

	@Documented(position = 1521,
			identifier = "answers_approved_by",
			value = "default",
			category = "Posts",
			description = "Controls who is able to mark an answer as accepted. "
					+ "Possible values are `default` (author and moderators), `admins` (admins only), `moderators` "
					+ "(moderators and admins).")
	public String answersApprovedBy() {
		return getConfigParam("answers_approved_by", "default");
	}

	@Documented(position = 1522,
			identifier = "answers_need_approval",
			value = "false",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the need for approval of new answers by a moderator. ")
	public boolean answersNeedApproval() {
		return getConfigBoolean("answers_need_approval", postsNeedApproval());
	}

	@Documented(position = 1530,
			identifier = "wiki_answers_enabled",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			tags = {"Pro"},
			description = "Enable/disable the ability for users to create wiki-style answers, editable by everyone.")
	public boolean wikiAnswersEnabled() {
		return getConfigBoolean("wiki_answers_enabled", true);
	}

	@Documented(position = 1540,
			identifier = "media_recording_allowed",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			tags = {"Pro"},
			description = "Enable/disable support for attaching recorded videos and voice messages to posts.")
	public boolean mediaRecordingAllowed() {
		return getConfigBoolean("media_recording_allowed", true);
	}

	@Documented(position = 1550,
			identifier = "delete_protection_enabled",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the ability for authors to delete their own question, when it already has "
					+ "answers and activity.")
	public boolean deleteProtectionEnabled() {
		return getConfigBoolean("delete_protection_enabled", true);
	}

	@Documented(position = 1560,
			identifier = "max_text_length",
			value = "20000",
			type = Integer.class,
			category = "Posts",
			description = "The maximum text length of each post (question or answer). Longer content will be truncated.")
	public int maxPostLength() {
		return getConfigInt("max_post_length", 20000);
	}

	@Documented(position = 1570,
			identifier = "max_tags_per_post",
			value = "5",
			type = Integer.class,
			category = "Posts",
			description = "The maximum number of tags a question can have. The minimum is 0 - then the default tag is used.")
	public int maxTagsPerPost() {
		return getConfigInt("max_tags_per_post", 5);
	}

	@Documented(position = 1571,
			identifier = "min_tags_per_post",
			value = "0",
			type = Integer.class,
			category = "Posts",
			description = "The minimum number of tags a question must have. The minimum is 0.")
	public int minTagsPerPost() {
		return getConfigInt("min_tags_per_post", 0);
	}

	@Documented(position = 1572,
			identifier = "tag_creation_allowed",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable tag creation by normal users. If disabled, only admins and moderators can create new tags.")
	public boolean tagCreationAllowed() {
		return getConfigBoolean("tag_creation_allowed", true);
	}

	@Documented(position = 1580,
			identifier = "max_replies_per_post",
			value = "500",
			type = Integer.class,
			category = "Posts",
			description = "The maximum number of answers a question can have.")
	public int maxRepliesPerPost() {
		return getConfigInt("max_replies_per_post", 500);
	}

	@Documented(position = 1590,
			identifier = "max_comments_per_id",
			value = "1000",
			type = Integer.class,
			category = "Posts",
			description = "The maximum number of comments a post can have.")
	public int maxCommentsPerPost() {
		return getConfigInt("max_comments_per_id", 1000);
	}

	@Documented(position = 1600,
			identifier = "max_comment_length",
			value = "600",
			type = Integer.class,
			category = "Posts",
			description = "The maximum length of each comment.")
	public int maxCommentLength() {
		return getConfigInt("max_comment_length", 600);
	}

	@Documented(position = 1610,
			identifier = "max_mentions_in_posts",
			value = "10",
			type = Integer.class,
			category = "Posts",
			tags = {"Pro"},
			description = "The maximum number of mentioned users a post can have.")
	public int maxMentionsInPosts() {
		return getConfigInt("max_mentions_in_posts", 10);
	}

	@Documented(position = 1620,
			identifier = "anonymous_posts_enabled",
			value = "false",
			type = Boolean.class,
			category = "Posts",
			tags = {"Pro"},
			description = "Enable/disable the ability for unathenticated users to create new questions.")
	public boolean anonymousPostsEnabled() {
		return getConfigBoolean("anonymous_posts_enabled", false);
	}

	@Documented(position = 1630,
			identifier = "nearme_feature_enabled",
			value = "false",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the ability for users to attach geolocation data to questions and "
					+ "location-based filtering of questions.")
	public boolean postsNearMeEnabled() {
		return getConfigBoolean("nearme_feature_enabled", !googleMapsApiKey().isEmpty());
	}

	@Documented(position = 1640,
			identifier = "merge_question_bodies",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the merging of question bodies when two questions are merged into one.")
	public boolean mergeQuestionBodies() {
		return getConfigBoolean("merge_question_bodies", true);
	}

	@Documented(position = 1650,
			identifier = "max_similar_posts",
			value = "7",
			type = Integer.class,
			category = "Posts",
			description = "The maximum number of similar posts which will be displayed on the side.")
	public int maxSimilarPosts() {
		return getConfigInt("max_similar_posts", 7);
	}

	@Documented(position = 1660,
			identifier = "default_question_tag",
			category = "Posts",
			description = "The default question tag, used when no other tags are provided by its author.")
	public String defaultQuestionTag() {
		return getConfigParam("default_question_tag", "");
	}

	@Documented(position = 1670,
			identifier = "posts_rep_threshold",
			value = "100",
			type = Integer.class,
			category = "Posts",
			description = "The minimum reputation an author needs to create a post without approval by moderators. "
					+ "This is only required if new posts need apporval.")
	public int postsReputationThreshold() {
		return getConfigInt("posts_rep_threshold", enthusiastIfHasRep());
	}

	/* **************************************************************************************************************
	 * Spaces                                                                                                Spaces *
	 ****************************************************************************************************************/

	@Documented(position = 1680,
			identifier = "auto_assign_spaces",
			category = "Spaces",
			description = "A comma-separated list of spaces to assign to all new users.")
	public String autoAssignSpaces() {
		return getConfigParam("auto_assign_spaces", "");
	}

	@Documented(position = 1681,
			identifier = "default_starting_space",
			category = "Spaces",
			description = "The starting space to be selected for all users upon sign in.")
	public String defaultStartingSpace() {
		return getConfigParam("default_starting_space", "");
	}

	@Documented(position = 1690,
			identifier = "reset_spaces_on_new_assignment",
			value = "true",
			type = Boolean.class,
			category = "Spaces",
			description = "Spaces delegated from identity providers will overwrite the existing ones for users.")
	public boolean resetSpacesOnNewAssignment(boolean def) {
		return getConfigBoolean("reset_spaces_on_new_assignment", def);
	}

	@Documented(position = 1691,
			identifier = "mods_access_all_spaces",
			value = "true",
			type = Boolean.class,
			category = "Spaces",
			description = "By default, moderators have access to and can edit content in all spaces. "
					+ "When disabled, moderators can only access the spaces they are assigned to by admins.")
	public boolean modsAccessAllSpaces() {
		return getConfigBoolean("mods_access_all_spaces", true);
	}

	/* **************************************************************************************************************
	 * Reputation and Rewards                                                                Reputation and Rewards *
	 ****************************************************************************************************************/

	@Documented(position = 1700,
			identifier = "answer_voteup_reward_author",
			value = "10",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of answer as reward when a user upvotes it.")
	public int answerVoteupRewardAuthor() {
		return getConfigInt("answer_voteup_reward_author", 10);
	}

	@Documented(position = 1710,
			identifier = "question_voteup_reward_author",
			value = "5",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of question as reward when a user upvotes it.")
	public int questionVoteupRewardAuthor() {
		return getConfigInt("question_voteup_reward_author", 5);
	}

	@Documented(position = 1720,
			identifier = "voteup_reward_author",
			value = "2",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of comment or other post as reward when a user upvotes it.")
	public int voteupRewardAuthor() {
		return getConfigInt("voteup_reward_author", 2);
	}

	@Documented(position = 1730,
			identifier = "answer_approve_reward_author",
			value = "10",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of answer as reward when the question's author accepts it.")
	public int answerApprovedRewardAuthor() {
		return getConfigInt("answer_approve_reward_author", 10);
	}

	@Documented(position = 1740,
			identifier = "answer_approve_reward_voter",
			value = "3",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of question who accepted an answer.")
	public int answerApprovedRewardVoter() {
		return getConfigInt("answer_approve_reward_voter", 3);
	}

	@Documented(position = 1741,
			identifier = "answer_create_reward_author",
			value = "5",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author who added an answer to a question (awarded once per question).")
	public int answerCreatedRewardAuthor() {
		return getConfigInt("answer_create_reward_author", 5);
	}

	@Documented(position = 1750,
			identifier = "post_votedown_penalty_author",
			value = "3",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points taken from author of post as penalty when their post was downvoted.")
	public int postVotedownPenaltyAuthor() {
		return getConfigInt("post_votedown_penalty_author", 3);
	}

	@Documented(position = 1760,
			identifier = "post_votedown_penalty_voter",
			value = "1",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points taken from the user who downvotes any content. Discourages downvoting slightly.")
	public int postVotedownPenaltyVoter() {
		return getConfigInt("post_votedown_penalty_voter", 1);
	}

	@Documented(position = 1770,
			identifier = "voter_ifhas",
			value = "100",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Number of votes (up or down) needed from a user for earning the `voter` badge.")
	public int voterIfHasRep() {
		return getConfigInt("voter_ifhas", 100);
	}

	@Documented(position = 1780,
			identifier = "commentator_ifhas",
			value = "100",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Number of comments a user needs to have posted for earning the `commentator` badge.")
	public int commentatorIfHasRep() {
		return getConfigInt("commentator_ifhas", 100);
	}

	@Documented(position = 1790,
			identifier = "critic_ifhas",
			value = "10",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Number of cast downvotes needed from a user for earning the `critic` badge.")
	public int criticIfHasRep() {
		return getConfigInt("critic_ifhas", 10);
	}

	@Documented(position = 1800,
			identifier = "supporter_ifhas",
			value = "50",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Number of cast upvotes needed from a user for earning the `supporter` badge.")
	public int supporterIfHasRep() {
		return getConfigInt("supporter_ifhas", 50);
	}

	@Documented(position = 1810,
			identifier = "goodquestion_ifhas",
			value = "20",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Votes needed on a question before its author gets to earn the `good question` badge.")
	public int goodQuestionIfHasRep() {
		return getConfigInt("goodquestion_ifhas", 20);
	}

	@Documented(position = 1820,
			identifier = "goodanswer_ifhas",
			value = "10",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Votes needed on an answer before its author gets to earn the `good answer` badge.")
	public int goodAnswerIfHasRep() {
		return getConfigInt("goodanswer_ifhas", 10);
	}

	@Documented(position = 1830,
			identifier = "enthusiast_ifhas",
			value = "100",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `enthusiast` badge.")
	public int enthusiastIfHasRep() {
		return getConfigInt("enthusiast_ifhas", 100);
	}

	@Documented(position = 1840,
			identifier = "freshman_ifhas",
			value = "300",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `freshman` badge.")
	public int freshmanIfHasRep() {
		return getConfigInt("freshman_ifhas", 300);
	}

	@Documented(position = 1850,
			identifier = "scholar_ifhas",
			value = "500",
			type = Boolean.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `scholar` badge.")
	public int scholarIfHasRep() {
		return getConfigInt("scholar_ifhas", 500);
	}

	@Documented(position = 1860,
			identifier = "teacher_ifhas",
			value = "1000",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `teacher` badge.")
	public int teacherIfHasRep() {
		return getConfigInt("teacher_ifhas", 1000);
	}

	@Documented(position = 1870,
			identifier = "",
			value = "5000",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `professor` badge.")
	public int professorIfHasRep() {
		return getConfigInt("professor_ifhas", 5000);
	}

	@Documented(position = 1880,
			identifier = "geek_ifhas",
			value = "9000",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `geek` badge.")
	public int geekIfHasRep() {
		return getConfigInt("geek_ifhas", 9000);
	}

	/* **************************************************************************************************************
	 * File Storage                                                                                    File Storage *
	 ****************************************************************************************************************/

	@Documented(position = 1890,
			identifier = "uploads_enabled",
			value = "true",
			type = Boolean.class,
			category = "File Storage",
			tags = {"Pro"},
			description = "Enable/disable file uploads.")
	public boolean uploadsEnabled() {
		return getConfigBoolean("uploads_enabled", true);
	}

	@Documented(position = 1900,
			identifier = "file_uploads_dir",
			value = "uploads",
			category = "File Storage",
			tags = {"Pro"},
			description = "The directory (local or in the cloud) where files will be stored.")
	public String fileUploadsDirectory() {
		return getConfigParam("file_uploads_dir", "uploads");
	}

	@Documented(position = 1910,
			identifier = "uploads_require_auth",
			value = "false",
			type = Boolean.class,
			category = "File Storage",
			tags = {"Pro"},
			description = "Enable/disable the requirement that uploaded files can only be accessed by authenticated users.")
	public boolean uploadsRequireAuthentication() {
		return getConfigBoolean("uploads_require_auth", !isDefaultSpacePublic());
	}

	@Documented(position = 1920,
			identifier = "allowed_upload_formats",
			category = "File Storage",
			tags = {"Pro"},
			description = "A comma-separated list of allowed MIME types in the format `extension:mime_type`, e.g."
					+ "`py:text/plain` or just the extensions `py,yml`")
	public String allowedUploadFormats() {
		return getConfigParam("allowed_upload_formats", "");
	}

	@Documented(position = 1930,
			identifier = "s3_bucket",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 bucket name as target for storing files.")
	public String s3Bucket() {
		return getConfigParam("s3_bucket", "");
	}

	@Documented(position = 1940,
			identifier = "s3_path",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 object prefix (directory) inside the bucket.")
	public String s3Path() {
		return getConfigParam("s3_path", "uploads");
	}

	@Documented(position = 1950,
			identifier = "s3_region",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 region.")
	public String s3Region() {
		return getConfigParam("s3_region", "");
	}

	@Documented(position = 1951,
			identifier = "s3_endpoint",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 endpoint override. The S3 region will be ignored if this is set. "
					+ "Can be used for connecting to S3-compatible storage providers.")
	public String s3Endpoint() {
		return getConfigParam("s3_endpoint", "");
	}

	@Documented(position = 1960,
			identifier = "s3_access_key",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 access key.")
	public String s3AccessKey() {
		return getConfigParam("s3_access_key", "");
	}

	@Documented(position = 1970,
			identifier = "s3_secret_key",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 secret key.")
	public String s3SecretKey() {
		return getConfigParam("s3_secret_key", "");
	}

	@Documented(position = 1980,
			identifier = "blob_storage_account",
			category = "File Storage",
			tags = {"Pro"},
			description = "Azure Blob Storage account ID.")
	public String azureStorageAccount() {
		return getConfigParam("blob_storage_account", "");
	}

	@Documented(position = 1990,
			identifier = "blob_storage_token",
			category = "File Storage",
			tags = {"Pro"},
			description = "Azure Blob Storage token.")
	public String azureStorageToken() {
		return getConfigParam("blob_storage_token", "");
	}

	@Documented(position = 2000,
			identifier = "blob_storage_container",
			category = "File Storage",
			tags = {"Pro"},
			description = "Azure Blob Storage container.")
	public String azureStorageContainer() {
		return getConfigParam("blob_storage_container", "");
	}

	@Documented(position = 2010,
			identifier = "blob_storage_path",
			category = "File Storage",
			tags = {"Pro"},
			description = "Azure Blob Storage path prefix (subfolder) within a container.")
	public String azureStoragePath() {
		return getConfigParam("blob_storage_path", "uploads");
	}

	/* **************************************************************************************************************
	 * Customization                                                                                  Customization *
	 ****************************************************************************************************************/

	@Documented(position = 2020,
			identifier = "default_language_code",
			category = "Customization",
			description = "The default language code to use for the site. Set this to make the site load a "
					+ "different language from English.")
	public String defaultLanguageCode() {
		return getConfigParam("default_language_code", "");
	}

	@Documented(position = 2030,
			identifier = "welcome_message",
			category = "Customization",
			description = "Adds a brief intro text inside a banner at the top of the main page for new visitors to see."
					+ "Not shown to authenticated users.")
	public String welcomeMessage() {
		return getConfigParam("welcome_message", "");
	}

	@Documented(position = 2040,
			identifier = "welcome_message_onlogin",
			category = "Customization",
			description = "Adds a brief intro text inside a banner at the top of the page. Shown to authenticated users only.")
	public String welcomeMessageOnLogin() {
		return getConfigParam("welcome_message_onlogin", "");
	}

	@Documented(position = 2041,
			identifier = "welcome_message_prelogin",
			category = "Customization",
			description = "Adds a brief intro text inside a banner at the top of the page. Shown only on the 'Sign in' page.")
	public String welcomeMessagePreLogin() {
		return getConfigParam("welcome_message_prelogin", "");
	}

	@Documented(position = 2050,
			identifier = "dark_mode_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the option for users to switch to the dark theme.")
	public boolean darkModeEnabled() {
		return getConfigBoolean("dark_mode_enabled", true);
	}

	@Documented(position = 2060,
			identifier = "meta_description",
			value = "Scoold is friendly place for knowledge sharing and collaboration...",
			category = "Customization",
			description = "The content inside the description `<meta>` tag.")
	public String metaDescription() {
		return getConfigParam("meta_description", appName() + " is friendly place for knowledge sharing and collaboration. "
				+ "Ask questions, post answers and comments, earn reputation points.");
	}

	@Documented(position = 2070,
			identifier = "meta_keywords",
			value = "knowledge base, knowledge sharing, collaboration, wiki...",
			category = "Customization",
			description = "The content inside the keywords `<meta>` tag.")
	public String metaKeywords() {
		return getConfigParam("meta_keywords", "knowledge base, knowledge sharing, collaboration, wiki, "
				+ "forum, Q&A, questions and answers, internal communication, project management, issue tracker, "
				+ "bug tracker, support tool");
	}

	@Documented(position = 2080,
			identifier = "show_branding",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the 'Powered by Scoold' branding in the footer.")
	public boolean scooldBrandingEnabled() {
		return getConfigBoolean("show_branding", true);
	}

	@Documented(position = 2090,
			identifier = "mathjax_enabled",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			tags = {"Pro"},
			description = "Enable/disable support for MathJax and LaTeX for scientific expressions in Markdown.")
	public boolean mathjaxEnabled() {
		return getConfigBoolean("mathjax_enabled", false);
	}

	@Documented(position = 2100,
			identifier = "gravatars_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable support for Gravatars.")
	public boolean gravatarsEnabled() {
		return getConfigBoolean("gravatars_enabled", true);
	}

	@Documented(position = 2110,
			identifier = "gravatars_pattern",
			value = "retro",
			category = "Customization",
			description = "The pattern to use when displaying empty/anonymous gravatar pictures.")
	public String gravatarsPattern() {
		return getConfigParam("gravatars_pattern", "retro");
	}

	@Documented(position = 2120,
			identifier = "avatar_repository",
			category = "Customization",
			tags = {"preview"},
			description = "The avatar repository - one of `imgur`, `cloudinary`.")
	public String avatarRepository() {
		return getConfigParam("avatar_repository", "");
	}

	@Documented(position = 2130,
			identifier = "footer_html",
			category = "Customization",
			description = "Some custom HTML content to be added to the website footer.")
	public String footerHtml() {
		return getConfigParam("footer_html", "");
	}

	@Documented(position = 2140,
			identifier = "navbar_link1_url",
			category = "Customization",
			description = "The URL of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink1Url() {
		return getConfigParam("navbar_link1_url", "");
	}

	@Documented(position = 2150,
			identifier = "navbar_link1_text",
			value = "Link1",
			category = "Customization",
			description = "The title of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink1Text() {
		return getConfigParam("navbar_link1_text", "Link1");
	}

	@Documented(position = 2151,
		identifier = "navbar_link1_target",
		category = "Customization",
		description = "The target attribute of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink1Target() {
		return getConfigParam("navbar_link1_target", "");
	}
	@Documented(position = 2160,
			identifier = "navbar_link2_url",
			category = "Customization",
			description = "The URL of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink2Url() {
		return getConfigParam("navbar_link2_url", "");
	}

	@Documented(position = 2170,
			identifier = "navbar_link2_text",
			value = "Link2",
			category = "Customization",
			description = "The title of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink2Text() {
		return getConfigParam("navbar_link2_text", "Link2");
	}

	@Documented(position = 2171,
		identifier = "navbar_link2_target",
		category = "Customization",
		description = "The target attribute of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink2Target() {
		return getConfigParam("navbar_link2_target", "");
	}

	@Documented(position = 2180,
			identifier = "navbar_menu_link1_url",
			category = "Customization",
			description = "The URL of an extra custom link which will be added to user's dropdown menu."
					+ " Only shown to authenticated users.")
	public String navbarCustomMenuLink1Url() {
		return getConfigParam("navbar_menu_link1_url", "");
	}

	@Documented(position = 2190,
			identifier = "navbar_menu_link1_text",
			value = "Menu Link1",
			category = "Customization",
			description = "The title of an extra custom link which will be added to the user's dropdown menu.")
	public String navbarCustomMenuLink1Text() {
		return getConfigParam("navbar_menu_link1_text", "Menu Link1");
	}

	@Documented(position = 2191,
		identifier = "navbar_menu_link1_target",
		category = "Customization",
		description = "The target attribute of an extra custom link which will be added to user's dropdown menu.")
	public String navbarCustomMenuLink1Target() {
		return getConfigParam("navbar_menu_link1_target", "");
	}

	@Documented(position = 2200,
			identifier = "navbar_menu_link2_url",
			category = "Customization",
			description = "The URL of an extra custom link which will be added to user's dropdown menu."
					+ " Only shown to authenticated users.")
	public String navbarCustomMenuLink2Url() {
		return getConfigParam("navbar_menu_link2_url", "");
	}

	@Documented(position = 2210,
			identifier = "navbar_menu_link2_text",
			value = "Menu Link2",
			category = "Customization",
			description = "The title of an extra custom link which will be added to the user's dropdown menu.")
	public String navbarCustomMenuLink2Text() {
		return getConfigParam("navbar_menu_link2_text", "Menu Link2");
	}

	@Documented(position = 2211,
		identifier = "navbar_menu_link2_target",
		category = "Customization",
		description = "The target attribute of an extra custom link which will be added to the user's dropdown menu.")
	public String navbarCustomMenuLink2Target() {
		return getConfigParam("navbar_menu_link2_target", "");
	}

	@Documented(position = 2220,
			identifier = "always_hide_comment_forms",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable a visual tweak which keeps all comment text editors closed at all times.")
	public boolean alwaysHideCommentForms() {
		return getConfigBoolean("always_hide_comment_forms", true);
	}

	@Documented(position = 2230,
			identifier = "footer_links_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable all links in the website footer.")
	public boolean footerLinksEnabled() {
		return getConfigBoolean("footer_links_enabled", true);
	}

	@Documented(position = 2240,
			identifier = "emails_footer_html",
			value = "<a href=\"{host_url}\">{app_name}</a> &bull; <a href=\"https://scoold.com\">Powered by Scoold</a>",
			category = "Customization",
			description = "The HTML code snippet to embed at the end of each transactional email message.")
	public String emailsFooterHtml() {
		return getConfigParam("emails_footer_html", "<a href=\"" + serverUrl() + serverContextPath() + "\">" +
				appName() + "</a> &bull; " + "<a href=\"https://scoold.com\">Powered by Scoold</a>");
	}

	@Documented(position = 2250,
			identifier = "cookie_consent_required",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the cookie consent popup box and blocks all external JS scripts from loading. "
					+ "Used for compliance with GDPR/CCPA.")
	public boolean cookieConsentRequired() {
		return getConfigBoolean("cookie_consent_required", false);
	}

	@Documented(position = 2260,
			identifier = "fixed_nav",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable a fixed navigation bar.")
	public boolean fixedNavEnabled() {
		return getConfigBoolean("fixed_nav", false);
	}

	@Documented(position = 2270,
			identifier = "logo_width",
			value = "100",
			type = Integer.class,
			category = "Customization",
			description = "The width of the logo image in the nav bar, in pixels. Used for fine adjustments to the logo size.")
	public int logoWidth() {
		return getConfigInt("logo_width", 100);
	}

	@Documented(position = 2280,
			identifier = "code_highlighting_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable support for syntax highlighting in code blocks.")
	public boolean codeHighlightingEnabled() {
		return getConfigBoolean("code_highlighting_enabled", true);
	}

	@Documented(position = 2290,
			identifier = "max_pages",
			value = "1000",
			type = Integer.class,
			category = "Customization",
			description = "Maximum number of pages to return as results.")
	public int maxPages() {
		return getConfigInt("max_pages", 1000);
	}

	@Documented(position = 2300,
			identifier = "numeric_pagination_enabled",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the numeric pagination style `(< 1 2 3...N >)`.")
	public boolean numericPaginationEnabled() {
		return getConfigBoolean("numeric_pagination_enabled", false);
	}

	@Documented(position = 2310,
			identifier = "html_in_markdown_enabled",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the ability for users to insert basic HTML tags inside Markdown content.")
	public boolean htmlInMarkdownEnabled() {
		return getConfigBoolean("html_in_markdown_enabled", false);
	}

	@Documented(position = 2320,
			identifier = "max_items_per_page",
			value = "30",
			type = Integer.class,
			category = "Customization",
			description = "Maximum number of results to return in a single page of results.")
	public int maxItemsPerPage() {
		return getConfigInt("max_items_per_page", Para.getConfig().maxItemsPerPage());
	}

	@Documented(position = 2330,
			identifier = "avatar_edits_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the ability for users to edit their profile pictures.")
	public boolean avatarEditsEnabled() {
		return getConfigBoolean("avatar_edits_enabled", true);
	}

	@Documented(position = 2340,
			identifier = "name_edits_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the ability for users to edit their name.")
	public boolean nameEditsEnabled() {
		return getConfigBoolean("name_edits_enabled", true);
	}

	/* **************************************************************************************************************
	 * Frontend Assets                                                                              Frontend Assets *
	 ****************************************************************************************************************/

	@Documented(position = 2350,
			identifier = "logo_url",
			value = "/images/logo.svg",
			category = "Frontend Assets",
			description = "The URL of the logo in the nav bar. Use a PNG, SVG, JPG or WebP format.")
	public String logoUrl() {
		return getConfigParam("logo_url", imagesLink() + "/logo.svg");
	}

	@Documented(position = 2351,
			identifier = "logo_dark_url",
			value = "/images/logo.svg",
			category = "Frontend Assets",
			description = "The URL of the logo in the nav bar used in dark mode. Use a PNG, SVG, JPG or WebP format.")
	public String logoDarkUrl() {
		return getConfigParam("logo_dark_url", logoUrl());
	}

	@Documented(position = 2360,
			identifier = "small_logo_url",
			value = "/images/logowhite.png",
			category = "Frontend Assets",
			description = "The URL of a smaller logo (only use PNG/JPG!). Used in transactional emails and the meta `og:image`.")
	public String logoSmallUrl() {
		return getConfigParam("small_logo_url", serverUrl() + imagesLink() + "/logowhite.png");
	}

	@Documented(position = 2370,
			identifier = "cdn_url",
			category = "Frontend Assets",
			description = "A CDN URL where all static assets might be stored.")
	public String cdnUrl() {
		return StringUtils.stripEnd(getConfigParam("cdn_url", serverContextPath()), "/");
	}

	@Documented(position = 2380,
			identifier = "stylesheet_url",
			value = "/styles/style.css",
			category = "Frontend Assets",
			description = "A stylesheet URL of a CSS file which will be used as the main stylesheet. *This will overwrite"
					+ " all existing CSS styles!*")
	public String stylesheetUrl() {
		return getConfigParam("stylesheet_url", stylesLink() + "/style.css");
	}

	@Documented(position = 2381,
			identifier = "dark_stylesheet_url",
			value = "/styles/dark.css",
			category = "Frontend Assets",
			description = "A stylesheet URL of a CSS file which will be used when dark mode is enabled. *This will overwrite"
					+ " all existing dark CSS styles!*")
	public String darkStylesheetUrl() {
		return getConfigParam("dark_stylesheet_url", stylesLink() + "/dark.css");
	}

	@Documented(position = 2390,
			identifier = "external_styles",
			category = "Frontend Assets",
			description = "A comma-separated list of external CSS files. These will be loaded *after* the main stylesheet.")
	public String externalStyles() {
		return getConfigParam("external_styles", "");
	}

	@Documented(position = 2400,
			identifier = "external_scripts._id_",
			type = Map.class,
			category = "Frontend Assets",
			description = "A map of external JS scripts. These will be loaded after the main JS script. For example: "
					+ "`scoold.external_scripts.script1 = \"alert('Hi')\"`")
	public Map<String, Object> externalScripts() {
		String prefix = "scoold_external_scripts_";
		Map<String, Object> ext = new LinkedHashMap<>(System.getenv().keySet().stream().
				filter(k -> k.startsWith(prefix)).collect(Collectors.
						toMap(mk -> StringUtils.removeStart(mk, prefix), mv -> System.getenv(mv))));
		if (getConfig().hasPath("external_scripts")) {
			ConfigObject extScripts = getConfig().getObject("external_scripts");
			if (extScripts != null && !extScripts.isEmpty()) {
				ext.putAll(extScripts.unwrapped());
			}
		}
		return ext;
	}

	@Documented(position = 2410,
			identifier = "inline_css",
			category = "Frontend Assets",
			description = "Some short, custom CSS snippet to embed inside the `<head>` element.")
	public String inlineCSS() {
		return getConfigParam("inline_css", "");
	}

	@Documented(position = 2420,
			identifier = "favicon_url",
			value = "/images/favicon.ico",
			category = "Frontend Assets",
			description = "The URL of the favicon image.")
	public String faviconUrl() {
		return getConfigParam("favicon_url", imagesLink() + "/favicon.ico");
	}

	@Documented(position = 2430,
			identifier = "meta_app_icon",
			value = "/images/logowhite.png",
			category = "Frontend Assets",
			description = "The URL of the app icon image in the `<meta property='og:image'>` tag.")
	public String metaAppIconUrl() {
		return getConfigParam("meta_app_icon", logoSmallUrl());
	}

	/* **************************************************************************************************************
	 * Mattermost Integration                                                                Mattermost Integration *
	 ****************************************************************************************************************/

	@Documented(position = 2431,
			identifier = "mattermost.auth_enabled",
			value = "false",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable authentication with Mattermost.")
	public boolean mattermostAuthEnabled() {
		return getConfigBoolean("mattermost.auth_enabled", !mattermostAppId().isEmpty());
	}

	@Documented(position = 2440,
			identifier = "mattermost.server_url",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Mattermost server URL.")
	public String mattermostServerUrl() {
		return getConfigParam("mattermost.server_url", "");
	}

	@Documented(position = 2450,
			identifier = "mattermost.bot_username",
			value = "scoold",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Mattermost bot username.")
	public String mattermostBotUsername() {
		return getConfigParam("mattermost.bot_username", "scoold");
	}

	@Documented(position = 2460,
			identifier = "mattermost.bot_icon_url",
			value = "/images/logowhite.png",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Mattermost bot avatar URL.")
	public String mattermostBotIconUrl() {
		return getConfigParam("mattermost.bot_icon_url", serverUrl() + imagesLink() + "/logowhite.png");
	}

	@Documented(position = 2470,
			identifier = "mattermost.post_to_space",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Default space on Scoold where questions created on Mattermost will be published. Set it to "
					+ "`workspace` for using the team's name.")
	public String mattermostPostToSpace() {
		return getConfigParam("mattermost.post_to_space", "");
	}

	@Documented(position = 2480,
			identifier = "mattermost.map_channels_to_spaces",
			value = "false",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Mattermost channels to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Mattermost channel.")
	public boolean mattermostMapChannelsToSpaces() {
		return getConfigBoolean("mattermost.map_channels_to_spaces", false);
	}

	@Documented(position = 2490,
			identifier = "mattermost.map_workspaces_to_spaces",
			value = "true",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Mattermost teams to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Mattermost team.")
	public boolean mattermostMapWorkspacesToSpaces() {
		return getConfigBoolean("mattermost.map_workspaces_to_spaces", true);
	}

	@Documented(position = 2500,
			identifier = "mattermost.max_notification_webhooks",
			value = "10",
			type = Integer.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a"
					+ " Mattermost channel to Scoold.")
	public int mattermostMaxNotificationWebhooks() {
		return getConfigInt("mattermost.max_notification_webhooks", 10);
	}

	@Documented(position = 2510,
			identifier = "mattermost.notify_on_new_answer",
			value = "true",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Mattermost for new answers.")
	public boolean mattermostNotifyOnNewAnswer() {
		return getConfigBoolean("mattermost.notify_on_new_answer", true);
	}

	@Documented(position = 2520,
			identifier = "mattermost.notify_on_new_question",
			value = "true",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Mattermost for new questions.")
	public boolean mattermostNotifyOnNewQuestion() {
		return getConfigBoolean("mattermost.notify_on_new_question", true);
	}

	@Documented(position = 2530,
			identifier = "mattermost.notify_on_new_comment",
			value = "true",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Mattermost for new comments.")
	public boolean mattermostNotifyOnNewComment() {
		return getConfigBoolean("mattermost.notify_on_new_comment", true);
	}

	@Documented(position = 2540,
			identifier = "mattermost.dm_on_new_comment",
			value = "false",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send direct messages to Mattermost users for new comments.")
	public boolean mattermostDmOnNewComment() {
		return getConfigBoolean("mattermost.dm_on_new_comment", false);
	}

	@Documented(position = 2550,
			identifier = "mattermost.default_question_tags",
			value = "via-mattermost",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Default question tags for questions created on Mattermost (comma-separated list).")
	public String mattermostDefaultQuestionTags() {
		return getConfigParam("mattermost.default_question_tags", "via-mattermost");
	}

	/* **************************************************************************************************************
	 * Slack Integration                                                                          Slack Integration *
	 ****************************************************************************************************************/

	@Documented(position = 2560,
			identifier = "slack.auth_enabled",
			value = "false",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable authentication with Slack.")
	public boolean slackAuthEnabled() {
		return getConfigBoolean("slack.auth_enabled", !slackAppId().isEmpty());
	}

	@Documented(position = 2570,
			identifier = "slack.app_id",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "The Slack app ID (first ID from the app's credentials, not the OAuth2 Client ID).")
	public String slackIntegrationAppId() {
		return getConfigParam("slack.app_id", "");
	}

	@Documented(position = 2580,
			identifier = "slack.signing_secret",
			value = "x",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Slack signing secret key for verifying request signatures.")
	public String slackSigningSecret() {
		return getConfigParam("slack.signing_secret", "x");
	}

	@Documented(position = 2590,
			identifier = "slack.max_notification_webhooks",
			value = "10",
			type = Integer.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a"
					+ " Slack channel to Scoold.")
	public int slackMaxNotificationWebhooks() {
		return getConfigInt("slack.max_notification_webhooks", 10);
	}

	@Documented(position = 2600,
			identifier = "slack.map_channels_to_spaces",
			value = "false",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Slack channels to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Slack channel.")
	public boolean slackMapChannelsToSpaces() {
		return getConfigBoolean("slack.map_channels_to_spaces", false);
	}

	@Documented(position = 2610,
			identifier = "slack.map_workspaces_to_spaces",
			value = "true",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Slack teams to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Slack team.")
	public boolean slackMapWorkspacesToSpaces() {
		return getConfigBoolean("slack.map_workspaces_to_spaces", true);
	}

	@Documented(position = 2620,
			identifier = "slack.post_to_space",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Default space on Scoold where questions created on Slack will be published. Set it to "
					+ "`workspace` for using the team's name.")
	public String slackPostToSpace() {
		return getConfigParam("slack.post_to_space", "");
	}

	@Documented(position = 2630,
			identifier = "slack.default_title",
			value = "A question from Slack",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Default question title for questions created on Slack.")
	public String slackDefaultQuestionTitle() {
		return getConfigParam("slack.default_title", "A question from Slack");
	}

	@Documented(position = 2640,
			identifier = "slack.notify_on_new_answer",
			value = "true",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Slack for new answers.")
	public boolean slackNotifyOnNewAnswer() {
		return getConfigBoolean("slack.notify_on_new_answer", true);
	}

	@Documented(position = 2650,
			identifier = "slack.notify_on_new_question",
			value = "true",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Slack for new questions.")
	public boolean slackNotifyOnNewQuestion() {
		return getConfigBoolean("slack.notify_on_new_question", true);
	}

	@Documented(position = 2660,
			identifier = "slack.notify_on_new_comment",
			value = "true",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Slack for new comments.")
	public boolean slackNotifyOnNewComment() {
		return getConfigBoolean("slack.notify_on_new_comment", true);
	}

	@Documented(position = 2670,
			identifier = "slack.dm_on_new_comment",
			value = "false",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send direct messages to Slack users for new comments.")
	public boolean slacDmOnNewComment() {
		return getConfigBoolean("slack.dm_on_new_comment", false);
	}

	@Documented(position = 2680,
			identifier = "slack.default_question_tags",
			value = "via-slack",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Default question tags for questions created on Slack (comma-separated list).")
	public String slackDefaultQuestionTags() {
		return getConfigParam("slack.default_question_tags", "via-slack");
	}

	/* **************************************************************************************************************
	 * Microsoft Teams Integration                                                      Microsoft Teams Integration *
	 ****************************************************************************************************************/

	@Documented(position = 2681,
			identifier = "teams.auth_enabled",
			value = "false",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			description = "Enable/disable authentication with Microsoft.")
	public boolean teamsAuthEnabled() {
		return getConfigBoolean("teams.auth_enabled", !microsoftAppId().isEmpty());
	}

	@Documented(position = 2690,
			identifier = "teams.bot_id",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Teams bot ID.")
	public String teamsBotId() {
		return getConfigParam("teams.bot_id", "");
	}

	@Documented(position = 2700,
			identifier = "teams.bot_secret",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Teams bot secret key.")
	public String teamsBotSecret() {
		return getConfigParam("teams.bot_secret", "");
	}

	@Documented(position = 2710,
			identifier = "teams.bot_service_url",
			value = "https://smba.trafficmanager.net/emea/",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Teams bot service URL.")
	public String teamsBotServiceUrl() {
		return getConfigParam("teams.bot_service_url", "https://smba.trafficmanager.net/emea/");
	}

	@Documented(position = 2720,
			identifier = "teams.notify_on_new_answer",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Teams for new answers.")
	public boolean teamsNotifyOnNewAnswer() {
		return getConfigBoolean("teams.notify_on_new_answer", true);
	}

	@Documented(position = 2730,
			identifier = "teams.notify_on_new_question",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Teams for new questions.")
	public boolean teamsNotifyOnNewQuestion() {
		return getConfigBoolean("teams.notify_on_new_question", true);
	}

	@Documented(position = 2740,
			identifier = "teams.notify_on_new_comment",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Teams for new comments.")
	public boolean teamsNotifyOnNewComment() {
		return getConfigBoolean("teams.notify_on_new_comment", true);
	}

	@Documented(position = 2750,
			identifier = "teams.dm_on_new_comment",
			value = "false",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send direct messages to Teams users for new comments.")
	public boolean teamsDmOnNewComment() {
		return getConfigBoolean("teams.dm_on_new_comment", false);
	}

	@Documented(position = 2760,
			identifier = "teams.post_to_space",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Default space on Scoold where questions created on Teams will be published. Set it to "
					+ "`workspace` for using the team's name.")
	public String teamsPostToSpace() {
		return getConfigParam("teams.post_to_space", "");
	}

	@Documented(position = 2770,
			identifier = "teams.map_channels_to_spaces",
			value = "false",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Teams channels to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Teams channel.")
	public boolean teamsMapChannelsToSpaces() {
		return getConfigBoolean("teams.map_channels_to_spaces", false);
	}

	@Documented(position = 2780,
			identifier = "teams.map_workspaces_to_spaces",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Teams teams to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Teams team.")
	public boolean teamsMapWorkspacesToSpaces() {
		return getConfigBoolean("teams.map_workspaces_to_spaces", true);
	}

	@Documented(position = 2781,
			identifier = "teams.private_teams_listing_allowed",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the listing of private teams on the Administration page "
					+ "when configuring notification webhooks for Scoold spaces.")
	public boolean teamsPrivateTeamsListingAllowed() {
		return getConfigBoolean("teams.private_teams_listing_allowed", true);
	}

	@Documented(position = 2790,
			identifier = "teams.max_notification_webhooks",
			value = "10",
			type = Integer.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a"
					+ " Teams channel to Scoold.")
	public int teamsMaxNotificationWebhooks() {
		return getConfigInt("teams.max_notification_webhooks", 10);
	}

	@Documented(position = 2800,
			identifier = "teams.default_title",
			value = "A question from Microsoft Teams",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Default question title for questions created on Teams.")
	public String teamsDefaultQuestionTitle() {
		return getConfigParam("teams.default_title", "A question from Microsoft Teams");
	}

	@Documented(position = 2810,
			identifier = "teams.default_question_tags",
			value = "via-teams",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Default question tags for questions created on Teams (comma-separated list).")
	public String teamsDefaultQuestionTags() {
		return getConfigParam("teams.default_question_tags", "via-teams");
	}

	/* **************************************************************************************************************
	 * SCIM                                                                                                    SCIM *
	 ****************************************************************************************************************/

	@Documented(position = 2820,
			identifier = "scim_enabled",
			value = "false",
			type = Boolean.class,
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "Enable/disable support for SCIM user provisioning.")
	public boolean scimEnabled() {
		return getConfigBoolean("scim_enabled", false);
	}

	@Documented(position = 2830,
			identifier = "scim_secret_token",
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "SCIM secret token.")
	public String scimSecretToken() {
		return getConfigParam("scim_secret_token", "");
	}

	@Documented(position = 2840,
			identifier = "scim_allow_provisioned_users_only",
			value = "false",
			type = Boolean.class,
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "Enable/disable the restriction that only SCIM-provisioned users can sign in.")
	public boolean scimAllowProvisionedUsersOnly() {
		return getConfigBoolean("scim_allow_provisioned_users_only", false);
	}

	@Documented(position = 2850,
			identifier = "scim_map_groups_to_spaces",
			value = "true",
			type = Boolean.class,
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "Enable/disable mapping of SCIM groups to Scoold spaces.")
	public boolean scimMapGroupsToSpaces() {
		return getConfigBoolean("scim_map_groups_to_spaces", true);
	}

	@Documented(position = 2860,
			identifier = "security.scim.admins_group_equivalent_to",
			value = "admins",
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "SCIM group whose members will be promoted to administrators on Scoold.")
	public String scimAdminsGroupEquivalentTo() {
		return getConfigParam("security.scim.admins_group_equivalent_to", "admins");
	}

	@Documented(position = 2870,
			identifier = "security.scim.mods_group_equivalent_to",
			value = "mods",
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "SCIM group whose members will be promoted to moderators on Scoold.")
	public String scimModeratorsGroupEquivalentTo() {
		return getConfigParam("security.scim.mods_group_equivalent_to", "mods");
	}

	/* **************************************************************************************************************
	 * Miscellaneous                                                                                  Miscellaneous *
	 ****************************************************************************************************************/

	@Documented(position = 2880,
			identifier = "security.redirect_uri",
			value = "http://localhost:8080",
			category = "Miscellaneous",
			description = "Publicly accessible, internet-facing URL of the Para endpoint where authenticated users "
					+ "will be redirected to, from the identity provider. Used when Para is hosted behind a proxy.")
	public String redirectUri() {
		return getConfigParam("security.redirect_uri", paraEndpoint());
	}

	@Documented(position = 2881,
			identifier = "security.hosturl_aliases",
			category = "Miscellaneous",
			description = "Provides a comma-separated list of alternative `host_url` public addresses to be used when "
					+ "returning from an authentication request to Para backend. This will override the hostname defined "
					+ "in `signin_success` and `signin_failure` and allow Scoold to run on multiple different public URLs "
					+ "while each separate server shares the same configuration. **Each must be a valid URL**")
	public String hostUrlAliases() {
		return getConfigParam("security.hosturl_aliases", "");
	}

	@Documented(position = 2890,
			identifier = "redirect_signin_to_idp",
			value = "false",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable the redirection of users from the signin page, directly to the IDP login page.")
	public boolean redirectSigninToIdp() {
		return getConfigBoolean("redirect_signin_to_idp", false);
	}

	@Documented(position = 2900,
			identifier = "gmaps_api_key",
			category = "Miscellaneous",
			description = "The Google Maps API key. Used for geolocation functionality, (e.g. 'posts near me', location).")
	public String googleMapsApiKey() {
		return getConfigParam("gmaps_api_key", "");
	}

	@Documented(position = 2910,
			identifier = "imgur_client_id",
			category = "Miscellaneous",
			tags = {"preview"},
			description = "Imgur API client id. Used for uploading avatars to Imgur. **Note:** Imgur have some breaking "
					+ "restrictions going on in their API and this might not work.")
	public String imgurClientId() {
		return getConfigParam("imgur_client_id", "");
	}

	@Documented(position = 2911,
		identifier = "cloudinary_url",
		category = "Miscellaneous",
		tags = {"preview"},
		description = "Cloudinary URL. Used for uploading avatars to Cloudinary.")
	public String cloudinaryUrl() {
		return getConfigParam("cloudinary_url", "");
	}

	@Documented(position = 2920,
			identifier = "max_fav_tags",
			value = "50",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Maximum number of favorite tags.")
	public int maxFavoriteTags() {
		return getConfigInt("max_fav_tags", 50);
	}

	@Documented(position = 2930,
			identifier = "batch_request_size",
			value = "0",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Maximum batch size for the Para client pagination requests.")
	public int batchRequestSize() {
		return getConfigInt("batch_request_size", 0);
	}

	@Documented(position = 2940,
			identifier = "signout_url",
			value = "/signin?code=5&success=true",
			category = "Miscellaneous",
			description = "The URL which users will be redirected to after they click 'Sign out'. Can be a page hosted"
					+ " externally.")
	public String signoutUrl(int... code) {
		if (code == null || code.length < 1) {
			code = new int[]{5};
		}
		return getConfigParam("signout_url", SIGNINLINK + "?code=" + code[0] + "&success=true");
	}

	@Documented(position = 2950,
			identifier = "vote_expires_after_sec",
			value = "2592000",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Vote expiration timeout, in seconds. Users can vote again on the same content after "
					+ "this period has elapsed. Default is 30 days.")
	public int voteExpiresAfterSec() {
		return getConfigInt("vote_expires_after_sec", Para.getConfig().voteExpiresAfterSec());
	}

	@Documented(position = 2960,
			identifier = "vote_locked_after_sec",
			value = "30",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Vote locking period, in seconds. Vote cannot be changed after this period has elapsed. "
					+ "Default is 30 sec.")
	public int voteLockedAfterSec() {
		return getConfigInt("vote_locked_after_sec", Para.getConfig().voteLockedAfterSec());
	}

	@Documented(position = 2961,
			identifier = "downvotes_enabled",
			value = "true",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable negative votes.")
	public boolean downvotesEnabled() {
		return getConfigBoolean("downvotes_enabled", true);
	}

	@Documented(position = 2970,
			identifier = "import_batch_size",
			value = "100",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Maximum number objects to read and send to Para when importing data from a backup.")
	public int importBatchSize() {
		return getConfigInt("import_batch_size", 100);
	}

	@Documented(position = 2980,
			identifier = "connection_retries_max",
			value = "10",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Maximum number of connection retries to Para.")
	public int paraConnectionRetryAttempts() {
		return getConfigInt("connection_retries_max", 10);
	}

	@Documented(position = 2990,
			identifier = "connection_retry_interval_sec",
			value = "10",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Para connection retry interval, in seconds.")
	public int paraConnectionRetryIntervalSec() {
		return getConfigInt("connection_retry_interval_sec", 10);
	}

	@Documented(position = 3000,
			identifier = "rewrite_inbound_links_with_fqdn",
			category = "Miscellaneous",
			description = "If set, links to Scoold in emails will be replaced with a public-facing FQDN.")
	public String rewriteInboundLinksWithFQDN() {
		return getConfigParam("rewrite_inbound_links_with_fqdn", "");
	}

	@Documented(position = 3010,
			identifier = "cluster_nodes",
			value = "1",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Total number of nodes present in the cluster when Scoold is deployed behind a reverse proxy.")
	public int clusterNodes() {
		return getConfigInt("cluster_nodes", 1);
	}

	@Documented(position = 3020,
			identifier = "autoinit.root_app_secret_key",
			category = "Miscellaneous",
			description = "If configured, Scoold will try to automatically initialize itself with Para and create its "
					+ "own Para app, called `app:scoold`. The keys for that new app will be saved in the configuration file.")
	public String autoInitWithRootAppSecretKey() {
		return getConfigParam("autoinit.root_app_secret_key", "");
	}

	@Documented(position = 3030,
			identifier = "autoinit.para_config_file",
			category = "Miscellaneous",
			description = "Does the same as `scoold.autoinit.root_app_secret_key` but tries to read the secret key for"
					+ " the root Para app from the Para configuration file, wherever that may be.")
	public String autoInitWithParaConfigFile() {
		return getConfigParam("autoinit.para_config_file", "");
	}

	@Documented(position = 3040,
			identifier = "sitemap_enabled",
			value = "true",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable the generation of `/sitemap.xml`.")
	public boolean sitemapEnabled() {
		return getConfigBoolean("sitemap_enabled", true);
	}

	@Documented(position = 3050,
			identifier = "access_log_enabled",
			value = "false",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable the Scoold access log.")
	public boolean accessLogEnabled() {
		return getConfigBoolean("access_log_enabled", false);
	}

	@Documented(position = 3060,
			identifier = "user_autocomplete_details_enabled",
			value = "false",
			type = Boolean.class,
			category = "Miscellaneous",
			tags = {"pro"},
			description = "Enable/disable extra details when displaying user results in autocomplete.")
	public boolean userAutocompleteDetailsEnabled() {
		return getConfigBoolean("user_autocomplete_details_enabled", false);
	}

	@Documented(position = 3070,
			identifier = "user_autocomplete_max_results",
			value = "10",
			type = Integer.class,
			category = "Miscellaneous",
			tags = {"pro"},
			description = "Controls the maximum number of search results in users' autocomplete.")
	public int userAutocompleteMaxResults() {
		return getConfigInt("user_autocomplete_max_results", 10);
	}

	@Documented(position = 3080,
			identifier = "users_discoverability_enabled",
			value = "true",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable discoverability of users on the site. If disabled, user profiles and the "
					+ "users page will be hidden for all except admins.")
	public boolean usersDiscoverabilityEnabled(boolean isAdmin) {
		return isAdmin || getConfigBoolean("users_discoverability_enabled", true);
	}

	@Documented(position = 3090,
			identifier = "notifications_as_reports_enabled",
			value = "false",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable copies of new content notifications in the form of reports on the site. "
					+ " Instead of checking their email, mods will be able to view and act on those on the reports page.")
	public boolean notificationsAsReportsEnabled() {
		return getConfigBoolean("notifications_as_reports_enabled", false);
	}

	@Documented(position = 3100,
			identifier = "akismet_api_key",
			category = "Miscellaneous",
			description = "API Key for Akismet for activating anti-spam protection of all posts.")
	public String akismetApiKey() {
		return getConfigParam("akismet_api_key", "");
	}

	@Documented(position = 3110,
			identifier = "automatic_spam_protection_enabled",
			value = "true",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable autonomous action taken against spam posts - if detected a spam post will be "
					+ "blocked without notice. By default, spam posts will require action and approval by admins.")
	public boolean automaticSpamProtectionEnabled() {
		return getConfigBoolean("automatic_spam_protection_enabled", true);
	}

	@Documented(position = 3120,
			identifier = "data_import_export_enabled",
			value = "true",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable backup and restore features on the Administration page.")
	public boolean dataImportExportEnabled() {
		return getConfigBoolean("data_import_export_enabled", true);
	}

	@Documented(position = 3130,
			identifier = "config_editing_enabled",
			value = "true",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable live configuration editing for admins on the Administration page.")
	public boolean configEditingEnabled() {
		return getConfigBoolean("config_editing_enabled", true);
	}

	/* **********************************************************************************************************/

	public boolean inDevelopment() {
		return environment().equals("development");
	}

	public boolean inProduction() {
		return environment().equals("production");
	}

	public boolean hasValue(String key) {
		return !StringUtils.isBlank(getConfigParam(key, ""));
	}

	private String getAppId() {
		return App.identifier(paraAccessKey());
	}

	public String localeCookie() {
		return getAppId() + "-locale";
	}

	public String spaceCookie() {
		return getAppId() + "-space";
	}

	public String authCookie() {
		return getAppId() + "-auth";
	}

	public String imagesLink() {
		return (inProduction() ? cdnUrl() : serverContextPath()) + "/images";
	}

	public String scriptsLink() {
		return (inProduction() ? cdnUrl() : serverContextPath()) + "/scripts";
	}

	public String stylesLink() {
		return (inProduction() ? cdnUrl() : serverContextPath()) + "/styles";
	}

	public Map<String, Object> oauthSettings(String alias) {
		String a = StringUtils.trimToEmpty(alias);
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("oa2" + a + "_app_id", oauthAppId(a));
		settings.put("oa2" + a + "_secret", oauthSecret(a));
		settings.put("security.oauth" + a + ".token_url", oauthTokenUrl(a));
		settings.put("security.oauth" + a + ".profile_url", oauthProfileUrl(a));
		settings.put("security.oauth" + a + ".scope", oauthScope(a));
		settings.put("security.oauth" + a + ".accept_header", oauthAcceptHeader(a));
		settings.put("security.oauth" + a + ".parameters.id", oauthIdParameter(a));
		settings.put("security.oauth" + a + ".parameters.name", oauthNameParameter(a));
		settings.put("security.oauth" + a + ".parameters.given_name", oauthGivenNameParameter(a));
		settings.put("security.oauth" + a + ".parameters.family_name", oauthFamiliNameParameter(a));
		settings.put("security.oauth" + a + ".parameters.email", oauthEmailParameter(a));
		settings.put("security.oauth" + a + ".parameters.picture", oauthPictureParameter(a));
		settings.put("security.oauth" + a + ".download_avatars", oauthAvatarDownloadingEnabled(a));
		settings.put("security.oauth" + a + ".domain", oauthDomain(a));
		settings.put("security.oauth" + a + ".token_delegation_enabled", oauthTokenDelegationEnabled(a));
		settings.put("security.oauth" + a + ".send_scope_to_token_endpoint", oauthSendScopeToTokenEndpointEnabled(a));
		return settings;
	}

	public Map<String, Object> ldapSettings() {
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("security.ldap.server_url", ldapServerUrl());
		settings.put("security.ldap.base_dn", ldapBaseDN());
		settings.put("security.ldap.bind_dn", ldapBindDN());
		settings.put("security.ldap.bind_pass", ldapBindPassword());
		settings.put("security.ldap.user_search_base", ldapUserSearchBase());
		settings.put("security.ldap.user_search_filter", ldapUserSearchFilter());
		settings.put("security.ldap.user_dn_pattern", ldapUserDNPattern());
		settings.put("security.ldap.password_attribute", ldapPasswordAttributeName());
		settings.put("security.ldap.username_as_name", ldapUsernameAsName());
		settings.put("security.ldap.ad_mode_enabled", ldapActiveDirectoryEnabled());
		settings.put("security.ldap.active_directory_domain", ldapActiveDirectoryDomain());
		settings.put("security.ldap.mods_group_node", ldapModeratorsGroupNode());
		settings.put("security.ldap.admins_group_node", ldapAdministratorsGroupNode());
		if (!ldapComparePasswords().isEmpty()) {
			settings.put("security.ldap.compare_passwords", ldapComparePasswords());
		}
		return settings;
	}

	public Map<String, Object> getParaAppSettings() {
		Map<String, Object> settings = new LinkedHashMap<String, Object>();
		settings.put("gp_app_id", googleAppId());
		settings.put("gp_secret", googleSecret());
		settings.put("fb_app_id", facebookAppId());
		settings.put("fb_secret", facebookSecret());
		settings.put("gh_app_id", githubAppId());
		settings.put("gh_secret", githubSecret());
		settings.put("in_app_id", linkedinAppId());
		settings.put("in_secret", linkedinSecret());
		settings.put("tw_app_id", twitterAppId());
		settings.put("tw_secret", twitterSecret());
		settings.put("ms_app_id", microsoftAppId());
		settings.put("ms_secret", microsoftSecret());
		settings.put("sl_app_id", slackAppId());
		settings.put("sl_secret", slackSecret());
		settings.put("az_app_id", amazonAppId());
		settings.put("az_secret", amazonSecret());
		// Microsoft tenant id support - https://github.com/Erudika/scoold/issues/208
		settings.put("ms_tenant_id", microsoftTenantId());
		// OAuth 2 settings
		settings.putAll(oauthSettings(""));
		settings.putAll(oauthSettings("second"));
		settings.putAll(oauthSettings("third"));
		// LDAP settings
		settings.putAll(ldapSettings());
		// secret key
		settings.put("app_secret_key", appSecretKey());
		// email verification
		settings.put("security.allow_unverified_emails", allowUnverifiedEmails());
		// sessions
		settings.put("security.one_session_per_user", oneSessionPerUser());
		settings.put("session_timeout", sessionTimeoutSec());

		// URLs for success and failure
		settings.put("security.hosturl_aliases", hostUrlAliases());
		settings.put("signin_success", serverUrl() + serverContextPath() + SIGNINLINK + "/success?jwt=id");
		settings.put("signin_failure", serverUrl() + serverContextPath() + SIGNINLINK + "?code=3&error=true");
		return settings;
	}

	String getDefaultContentSecurityPolicy() {
		return "default-src 'self'; "
				+ "base-uri 'self'; "
				+ "media-src 'self' blob:; "
				+ "form-action 'self' " + serverUrl() + serverContextPath() + SIGNOUTLINK + "; "
				+ "connect-src 'self' " + (inProduction() ? serverUrl() : "")
				+ " maps.googleapis.com api.imgur.com api.cloudinary.com accounts.google.com " + cspConnectSources() + "; "
				+ "frame-src 'self' *.google.com " + cspFrameSources() + "; "
				+ "frame-ancestors 'self'; "
				+ "font-src 'self' cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com " + cspFontSources() + "; "
				// unsafe-inline required by MathJax and Google Maps!
				+ "style-src 'self' 'unsafe-inline' fonts.googleapis.com accounts.google.com "
				+ (cdnUrl().startsWith("/") ? "" : cdnUrl() + " ") + cspStyleSources() + "; "
				+ "img-src 'self' https: data:; "
				+ "object-src 'none'; "
				+ "report-uri /reports/cspv; "
				+ "script-src 'unsafe-inline' https: 'nonce-{{nonce}}' 'strict-dynamic';"; // CSP2 backward compatibility
	}

}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.App;
import com.erudika.para.core.email.Emailer;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.utils.CoreUtils;
import com.erudika.scoold.utils.ScooldEmailer;
import com.erudika.scoold.utils.ScooldRequestInterceptor;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.velocity.VelocityConfigurer;
import com.erudika.scoold.velocity.VelocityViewResolver;
import jakarta.inject.Named;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@SpringBootApplication
public class ScooldServer extends SpringBootServletInitializer {

	static {
		System.setProperty("scoold.scoold", "-"); // prevents empty config
	}

	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	static {
		// tells ParaClient where to look for classes that implement ParaObject
		CoreUtils.registerCoreClasses();
		System.setProperty("server.port", String.valueOf(CONF.serverPort()));
		System.setProperty("server.servlet.context-path", CONF.serverContextPath());
		System.setProperty("server.use-forward-headers", String.valueOf(CONF.inProduction()));
		System.setProperty("para.logs_name", CONF.getConfigRootPrefix());
		if (CONF.accessLogEnabled()) {
			System.setProperty("server.jetty.accesslog.append", "true");
			System.setProperty("server.jetty.accesslog.enabled", "true");
			if (!System.getProperty("scoold.file_logger_level", "INFO").equalsIgnoreCase("OFF")) {
				System.setProperty("server.jetty.accesslog.filename", System.getProperty("para.logs_dir", ".")
						+ File.separator + CONF.getConfigRootPrefix() + "-access.log");
			}
		}
	}

	public static final String TOKEN_PREFIX = "ST_";
	public static final String HOMEPAGE = "/";
	public static final String AUTH_USER_ATTRIBUTE = TOKEN_PREFIX + "AUTH_USER";
	public static final String REST_ENTITY_ATTRIBUTE = "REST_ENTITY";
	public static final String PEOPLELINK = HOMEPAGE + "people";
	public static final String PROFILELINK = HOMEPAGE + "profile";
	public static final String SEARCHLINK = HOMEPAGE + "search";
	public static final String SIGNINLINK = HOMEPAGE + "signin";
	public static final String SIGNOUTLINK = HOMEPAGE + "signout";
	public static final String ABOUTLINK = HOMEPAGE + "about";
	public static final String PRIVACYLINK = HOMEPAGE + "privacy";
	public static final String TERMSLINK = HOMEPAGE + "terms";
	public static final String TAGSLINK = HOMEPAGE + "tags";
	public static final String SETTINGSLINK = HOMEPAGE + "settings";
	public static final String REPORTSLINK = HOMEPAGE + "reports";
	public static final String ADMINLINK = HOMEPAGE + "admin";
	public static final String VOTEDOWNLINK = HOMEPAGE + "votedown";
	public static final String VOTEUPLINK = HOMEPAGE + "voteup";
	public static final String QUESTIONLINK = HOMEPAGE + "question";
	public static final String QUESTIONSLINK = HOMEPAGE + "questions";
	public static final String COMMENTLINK = HOMEPAGE + "comment";
	public static final String POSTLINK = HOMEPAGE + "post";
	public static final String REVISIONSLINK = HOMEPAGE + "revisions";
	public static final String FEEDBACKLINK = HOMEPAGE + "feedback";
	public static final String LANGUAGESLINK = HOMEPAGE + "languages";
	public static final String APIDOCSLINK = HOMEPAGE + "apidocs";

	private static final Logger logger = LoggerFactory.getLogger(ScooldServer.class);

	public static void main(String[] args) {
		builder(new SpringApplicationBuilder(), false).run(args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder app) {
		return builder(app, true);
	}

	static SpringApplicationBuilder builder(SpringApplicationBuilder b, boolean isWar, Class<?>... sources) {
		initConfig();
		b.sources(ScooldServer.class);
		b.sources(sources);
		b.profiles(CONF.environment());
		b.web(WebApplicationType.SERVLET);
		return b;
	}

	private static void initConfig() {
		System.setProperty("server.servlet.session.timeout", String.valueOf(CONF.sessionTimeoutSec()));
		// JavaMail configuration properties
		System.setProperty("spring.mail.host", CONF.mailHost());
		System.setProperty("spring.mail.port", String.valueOf(CONF.mailPort()));
		System.setProperty("spring.mail.username", CONF.mailUsername());
		System.setProperty("spring.mail.password", CONF.mailPassword());
		System.setProperty("spring.mail.properties.mail.smtp.starttls.enable", Boolean.toString(CONF.mailTLSEnabled()));
		System.setProperty("spring.mail.properties.mail.smtp.ssl.enable", Boolean.toString(CONF.mailSSLEnabled()));
		System.setProperty("spring.mail.properties.mail.debug", Boolean.toString(CONF.mailDebugEnabled()));
	}

	@Bean
	public WebMvcConfigurer baseConfigurerBean(@Named final ScooldRequestInterceptor sri) {
		return new WebMvcConfigurer() {
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(sri);
			}
		};
	}

	@Bean
	public ParaClient paraClientBean() {
		tryAutoInitParaApp();
		logger.info("Scoold server is listening on {}", CONF.serverUrl() + CONF.serverContextPath());
		String accessKey = CONF.paraAccessKey();
		ParaClient pc = new ParaClient(accessKey, CONF.paraSecretKey());
		ScooldUtils.setParaEndpointAndApiPath(pc);
		pc.setChunkSize(CONF.batchRequestSize()); // unlimited batch size

		printRootAppConnectionNotice();
		printGoogleMigrationNotice();
		printFacebookMigrationNotice();
		printParaConfigChangeNotice();

		ScooldUtils.tryConnectToPara(() -> {
			pc.throwExceptionOnHTTPError(true);
			// update the Scoold App settings through the Para App settings API.
			pc.setAppSettings(CONF.getParaAppSettings());
			pc.throwExceptionOnHTTPError(false);
			boolean connected = pc.getTimestamp() > 0; // finally, check if app actually exists
			if (connected) {
				logger.info("Connected to Para on {} with credentials for '{}'.", CONF.paraEndpoint(), accessKey);
			}
			return connected;
		});
		return pc;
	}

	@Bean
	public Emailer scooldEmailerBean(JavaMailSender mailSender) {
		return new ScooldEmailer(mailSender);
	}

	/**
	 * @return Velocity config bean
	 */
	@Bean
	public VelocityConfigurer velocityConfigBean() {
		Properties velocityProperties = new Properties();
		velocityProperties.put(RuntimeConstants.VM_LIBRARY, "macro.vm");
		velocityProperties.put(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, CONF.inProduction());
		velocityProperties.put(RuntimeConstants.VM_LIBRARY_AUTORELOAD, !CONF.inProduction());
		velocityProperties.put(RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, true);
		velocityProperties.put(RuntimeConstants.EVENTHANDLER_REFERENCEINSERTION,
				"org.apache.velocity.app.event.implement.EscapeHtmlReference");
		velocityProperties.put("eventhandler.escape.html.match", "^((?!_).)+$");

		VelocityConfigurer vc = new VelocityConfigurer();
		vc.setVelocityProperties(velocityProperties);
		vc.setResourceLoaderPath("classpath:templates/");
		vc.setPreferFileSystemAccess(!CONF.inProduction()); // use SpringResourceLoader only in production
		return vc;
	}

	@Bean
	public ViewResolver viewResolver() {
		VelocityViewResolver viewr = new VelocityViewResolver();
		viewr.setRedirectHttp10Compatible(false);
		viewr.setSuffix(".vm");
		viewr.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return viewr;
	}

	/**
	 * @return Error page registry bean
	 */
	@Bean
	public ErrorPageRegistrar errorPageRegistrar() {
		return new ErrorPageRegistrar() {
			@Override
			public void registerErrorPages(ErrorPageRegistry epr) {
				epr.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/not-found"));
				epr.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN, "/error/403"));
				epr.addErrorPages(new ErrorPage(HttpStatus.UNAUTHORIZED, "/error/401"));
				epr.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500"));
				epr.addErrorPages(new ErrorPage(HttpStatus.SERVICE_UNAVAILABLE, "/error/503"));
				epr.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/error/400"));
				epr.addErrorPages(new ErrorPage(HttpStatus.METHOD_NOT_ALLOWED, "/error/405"));
				epr.addErrorPages(new ErrorPage(Exception.class, "/error/500"));
			}
		};
	}

	private void printGoogleMigrationNotice() {
		if (CONF.hasValue("google_client_id") && StringUtils.isBlank(CONF.googleAppId())) {
			logger.warn("Please migrate to the standard OAuth2 method for authenticating with Google. "
					+ "Change '{}.google_client_id' to '{}.gp_app_id' and also add the secret key for your OAuth2 "
					+ "app as '{}.gp_secret' in your configuration. https://console.cloud.google.com/apis/credentials",
					CONF.getConfigRootPrefix(), CONF.getConfigRootPrefix(), CONF.getConfigRootPrefix());
		}
	}

	private void printFacebookMigrationNotice() {
		if (!StringUtils.isBlank(CONF.facebookAppId()) && StringUtils.isBlank(CONF.facebookSecret())) {
			logger.warn("Please migrate to the standard OAuth2 method for authenticating with Facebook. "
					+ "Secret key is missing - add the secret key for your OAuth2 "
					+ "app as '{}.fb_secret' in your configuration. "
					+ "https://developers.facebook.com/apps/896508060362903/settings/basic/",
					CONF.getConfigRootPrefix(), CONF.facebookAppId());
		}
	}

	private void printRootAppConnectionNotice() {
		if (App.id(Config.PARA).equalsIgnoreCase(App.id(CONF.paraAccessKey()))) {
			logger.warn("You are connected to the root Para app - this is not recommended and can be problematic. "
					+ "Please create a separate Para app for Scoold to connect to.");
		}
	}

	private void printParaConfigChangeNotice() {
		String paraPrefix = Para.getConfig().getConfigRootPrefix();
		String scooldPrefix = CONF.getConfigRootPrefix();
		com.typesafe.config.Config paraPath = Para.getConfig().getConfig().atPath(paraPrefix);
		com.typesafe.config.Config scooldPath = CONF.getConfig().atPath(scooldPrefix);
		for (String confKey : Arrays.asList("access_key", "secret_key", "endpoint")) {
			if (paraPath.hasPath(paraPrefix + "." + confKey)) {
				logger.warn("Found deprecated configuration property '{}.{}' - please rename all Scoold "
						+ "configuration properties to start with prefix '{}', e.g. '{}.{}'.",
						paraPrefix, confKey, scooldPrefix, scooldPrefix, confKey);
			} else if (scooldPath.hasPath(scooldPrefix + "." + confKey)) {
				logger.warn("Found deprecated configuration property '{}.{}' - "
						+ "please rename it to '{}.para_{}'.", scooldPrefix, confKey, scooldPrefix, confKey);
			}
		}
	}

	private void tryAutoInitParaApp() {
		String rootSecret = null;
		String confFile = CONF.getConfigFilePath();
		if (!CONF.autoInitWithRootAppSecretKey().isBlank()) {
			rootSecret = CONF.autoInitWithRootAppSecretKey().trim();
		} else if (!CONF.autoInitWithParaConfigFile().isBlank()) {
			com.typesafe.config.Config paraConfig =
					Config.parseFileWithoutIncludes(new File(CONF.autoInitWithParaConfigFile()));
			if (paraConfig.hasPath("para.root_secret_key")) {
				rootSecret = paraConfig.getString("para.root_secret_key");
			}
		}
		if (rootSecret != null) {
			ParaClient pcRoot = new ParaClient(App.id(Config.PARA), rootSecret);
			ScooldUtils.setParaEndpointAndApiPath(pcRoot);
			String childApp = CONF.paraAccessKey();
			boolean connectionOk = pcRoot.getTimestamp() > 0;
			if (connectionOk && (pcRoot.getCount("app") == 1 || pcRoot.read(childApp) == null)) {
				Map<String, Object> credentials = pcRoot.invokeGet("_setup/" +
						Utils.urlEncode(App.identifier(childApp)), null, Map.class);
				if (credentials.containsKey("accessKey") && credentials.containsKey("secretKey")) {
					String acceessKey = (String) credentials.get("accessKey");
					System.setProperty("scoold.para_access_key", acceessKey);
					System.setProperty("scoold.para_secret_key", (String) credentials.get("secretKey"));
					if (StringUtils.isBlank(CONF.appSecretKey())) {
						System.setProperty("scoold.app_secret_key", Utils.generateSecurityToken(32));
					}
					logger.info("Auto-init succeeded - created new app '{}' and saved keys to {}.", acceessKey, confFile);
					CONF.store();
				}
			} else if (!connectionOk) {
				logger.error("Failed to auto-initialize {} - try updating your app's credentials manually.", childApp);
			}
		}
		if (StringUtils.isBlank(System.getProperty("config.url")) && !Files.exists(Paths.get(confFile).toAbsolutePath())) {
			System.setProperty("scoold.app_name", CONF.appName());
			logger.info("No configuration file found - configuration saved in {}.", confFile);
			CONF.store();
		}
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class UnapprovedQuestion extends Question {
	private static final long serialVersionUID = 1L;
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Vote;
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import com.erudika.scoold.ScooldServer;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public abstract class Post extends Sysprop {

	private static final long serialVersionUID = 1L;
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	public static final String DEFAULT_SPACE = "scooldspace:default";
	public static final String ALL_MY_SPACES = "scooldspace:*";

	@Stored
	private String body;
	@Stored @NotBlank @Size(min = 2, max = 255)
	private String title;
	@Stored @NotEmpty @Size(min = 1)
	private List<String> tags;

	@Stored private Long viewcount;
	@Stored private String answerid;
	@Stored private String revisionid;
	@Stored private String closerid;
	@Stored private Long answercount;
	@Stored private Long lastactivity;
	@Stored private Long lastedited;
	@Stored private String lasteditby;
	@Stored private String deletereportid;
	@Stored private String location;
	@Stored private String address;
	@Stored private String latlng;
	@Stored private List<String> commentIds;
	@Stored private String space;
	@Stored private Map<String, String> followers;
	@Stored private Boolean deprecated;
	@Stored private Long approvalTimestamp;

	private transient Profile author;
	private transient Profile lastEditor;
	private transient Profile approvedby;
	private transient List<Comment> comments;
	private transient Pager itemcount;
	private transient Vote vote;
	private transient boolean spam;

	public Post() {
		this.answercount = 0L;
		this.viewcount = 0L;
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	public Long getApprovalTimestamp() {
		return approvalTimestamp;
	}

	public void setApprovalTimestamp(Long approvalTimestamp) {
		this.approvalTimestamp = approvalTimestamp;
	}

	public Profile getApprovedby() {
		return approvedby;
	}

	public void setApprovedby(Profile approvedby) {
		this.approvedby = approvedby;
	}

	public Boolean getDeprecated() {
		if (deprecated == null || isReply()) {
			deprecated = false;
		}
		return deprecated;
	}

	public void setDeprecated(Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public Long getLastactivity() {
		if (lastactivity == null || lastactivity <= 0) {
			lastactivity = getUpdated();
		}
		return lastactivity;
	}

	public void setLastactivity(Long lastactivity) {
		this.lastactivity = lastactivity;
	}

	public Long getLastedited() {
		if (lastedited == null || lastedited <= 0) {
			lastedited = getUpdated();
		}
		return lastedited;
	}

	public void setLastedited(Long lastedited) {
		this.lastedited = lastedited;
	}

	@JsonIgnore
	public Pager getItemcount() {
		if (itemcount == null) {
			itemcount = new Pager(5);
			itemcount.setDesc(false);
		}
		return itemcount;
	}

	public void setItemcount(Pager itemcount) {
		this.itemcount = itemcount;
	}

	public Vote getVote() {
		return vote;
	}

	public void setVote(Vote vote) {
		this.vote = vote;
	}

	public Map<String, String> getFollowers() {
		return followers;
	}

	public void setFollowers(Map<String, String> followers) {
		this.followers = followers;
	}

	public String getDeletereportid() {
		return deletereportid;
	}

	public void setDeletereportid(String deletereportid) {
		this.deletereportid = deletereportid;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getLatlng() {
		return latlng;
	}

	public void setLatlng(String latlng) {
		this.latlng = latlng;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getLasteditby() {
		return lasteditby;
	}

	public void setLasteditby(String lasteditby) {
		this.lasteditby = lasteditby;
	}

	public Long getAnswercount() {
		return answercount;
	}

	public void setAnswercount(Long answercount) {
		this.answercount = answercount;
	}

	public String getCloserid() {
		return closerid;
	}

	public void setCloserid(String closed) {
		this.closerid = closed;
	}

	public String getRevisionid() {
		return revisionid;
	}

	public void setRevisionid(String revisionid) {
		this.revisionid = revisionid;
	}

	public String getAnswerid() {
		return answerid;
	}

	public void setAnswerid(String answerid) {
		this.answerid = answerid;
	}

	public Long getViewcount() {
		return viewcount;
	}

	public void setViewcount(Long viewcount) {
		this.viewcount = viewcount;
	}

	public String getTitle() {
		if (StringUtils.isBlank(title)) {
			return getId();
		}
		return title;
	}

	public void setTitle(String title) {
		if (!StringUtils.isBlank(title)) {
			this.title = StringUtils.trimToEmpty(title);
			setName(title);
		}
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isSpam() {
		return spam;
	}

	public void setSpam(boolean spam) {
		this.spam = spam;
	}

	public boolean isClosed() {
		return !StringUtils.isBlank(this.closerid);
	}

	public String getTagsString() {
		if (getTags() == null || getTags().isEmpty()) {
			return "";
		}
		Collections.sort(getTags());
		return StringUtils.join(getTags(), ",");
	}

	public String create() {
		updateTags(null, getTags());
		this.body = Utils.abbreviate(this.body, ScooldUtils.getConfig().maxPostLength());
		Post p = client().create(this);
		if (p != null) {
			Revision.createRevisionFromPost(p, true);
			setId(p.getId());
			setTimestamp(p.getTimestamp());
			return p.getId();
		}
		return null;
	}

	public void update() {
		client().update(this);
	}

	public void delete() {
		// delete post
		ArrayList<ParaObject> children = new ArrayList<ParaObject>();
		ArrayList<String> ids = new ArrayList<String>();
		// delete Comments
		children.addAll(client().getChildren(this, Utils.type(Comment.class)));
		// delete Revisions
		children.addAll(client().getChildren(this, Utils.type(Revision.class)));

		for (ParaObject reply : client().getChildren(this, Utils.type(Reply.class))) {
			// delete answer
			children.add(reply);
			// delete Comments
			children.addAll(client().getChildren(reply, Utils.type(Comment.class)));
			// delete Revisions
			children.addAll(client().getChildren(reply, Utils.type(Revision.class)));
		}
		for (ParaObject child : children) {
			ids.add(child.getId());
		}
		updateTags(getTags(), null);
		client().deleteAll(ids);
		client().delete(this);
	}

	public static String getTagString(String tag) {
		return StringUtils.truncate(tag, 35);
	}

	public void updateTags(List<String> oldTags, List<String> newTags) {
		List<String> deleteUs = new LinkedList<>();
		List<Tag> updateUs = new LinkedList<>();
		Map<String, Tag> oldTagz = Optional.ofNullable(oldTags).orElse(Collections.emptyList()).stream().
				map(t -> new Tag(getTagString(t))).distinct().collect(Collectors.toMap(t -> t.getId(), t -> t));
		Map<String, Tag> newTagz = Optional.ofNullable(newTags).orElse(Collections.emptyList()).stream().
				map(t -> new Tag(getTagString(t))).distinct().collect(Collectors.toMap(t -> t.getId(), t -> t));
		Map<String, Tag> existingTagz = client().readAll(Stream.concat(oldTagz.keySet().stream(), newTagz.keySet().
				stream()).distinct().collect(Collectors.toList())).
				stream().collect(Collectors.toMap(t -> t.getId(), t -> (Tag) t));
		// add newly created tags
		if (CONF.tagCreationAllowed() || ScooldUtils.getInstance().isMod(getAuthor())) {
			client().createAll(newTagz.values().stream().filter(t -> {
				t.setCount(1);
				return !existingTagz.containsKey(t.getId());
			}).collect(Collectors.toList()));
		} else {
			Iterator<Map.Entry<String, Tag>> it = newTagz.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Tag> entry = it.next();
				if (!existingTagz.containsKey(entry.getKey())) {
					it.remove();
				}
			}
		}
		// increment or decrement the count of the rest
		existingTagz.values().forEach(t -> {
			if (!oldTagz.containsKey(t.getId()) && newTagz.containsKey(t.getId())) {
				t.setCount(t.getCount() + 1);
				updateUs.add(t);
			} else if (oldTagz.containsKey(t.getId()) && (newTags == null || !newTagz.containsKey(t.getId()))) {
				t.setCount(t.getCount() - 1);
				if (t.getCount() <= 0) {
					// check if actual count is different
					int c = client().getCount(Utils.type(Question.class),
							Collections.singletonMap(Config._TAGS, t.getTag())).intValue();
					if (c <= 1) {
						deleteUs.add(t.getId());
					} else {
						t.setCount(c);
					}
				} else {
					updateUs.add(t);
				}
			} // else: count remains unchanged
		});
		client().updateAll(updateUs);
		client().deleteAll(deleteUs);
		int tagsLimit = Math.min(CONF.maxTagsPerPost(), 100);
		setTags(newTagz.values().stream().limit(tagsLimit).map(t -> t.getTag()).collect(Collectors.toList()));
	}

	@JsonIgnore
	public Profile getAuthor() {
		return author;
	}

	public void setAuthor(Profile author) {
		this.author = author;
	}

	@JsonIgnore
	public Profile getLastEditor() {
		return lastEditor;
	}

	public void setLastEditor(Profile lastEditor) {
		this.lastEditor = lastEditor;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}

	@JsonIgnore // DO NOT REMOVE! clashes with User.getComments() field in index
	public List<Comment> getComments() {
		return this.comments;
	}

	public List<String> getCommentIds() {
		return commentIds;
	}

	public String getSpace() {
		if (StringUtils.isBlank(space)) {
			space = DEFAULT_SPACE;
		}
		return space;
	}

	public void setSpace(String space) {
		this.space = space;
	}

	public void setCommentIds(List<String> commentIds) {
		this.commentIds = commentIds;
	}

	public boolean addCommentId(String id) {
		if (getCommentIds() != null && getCommentIds().size() < getItemcount().getLimit()) {
			return getCommentIds().add(id);
		}
		return false;
	}

	@JsonIgnore
	public List<Reply> getAnswers(Pager pager) {
		return getAnswers(Reply.class, pager);
	}

	@JsonIgnore
	public List<Reply> getUnapprovedAnswers(Pager pager) {
		if (isReply()) {
			return Collections.emptyList();
		}
		return client().getChildren(this, Utils.type(UnapprovedReply.class), pager);
	}

	private List<Reply> getAnswers(Class<? extends Reply> type, Pager pager) {
		if (isReply()) {
			return Collections.emptyList();
		}

		List<Reply> answers = client().getChildren(this, Utils.type(type), pager);
		// we try to find the accepted answer inside the answers list, in not there, read it from db
		if (pager.getPage() < 2 && !StringUtils.isBlank(getAnswerid())) {
			Reply acceptedAnswer = null;
			for (Iterator<Reply> iterator = answers.iterator(); iterator.hasNext();) {
				Reply answer = iterator.next();
				if (getAnswerid().equals(answer.getId())) {
					acceptedAnswer = answer;
					iterator.remove();
					break;
				}
			}
			if (acceptedAnswer == null) {
				acceptedAnswer = client().read(getAnswerid());
			}
			if (acceptedAnswer != null) {
				ArrayList<Reply> sortedAnswers = new ArrayList<Reply>(answers.size() + 1);
				if (pager.isDesc()) {
					sortedAnswers.add(acceptedAnswer);
					sortedAnswers.addAll(answers);
				} else {
					sortedAnswers.addAll(answers);
					sortedAnswers.add(acceptedAnswer);
				}
				return sortedAnswers;
			}
		}
		return answers;
	}

	@JsonIgnore
	public List<Revision> getRevisions(Pager pager) {
		return client().getChildren(this, Utils.type(Revision.class), pager);
	}

	@JsonIgnore
	public boolean isReply() {
		return this instanceof Reply;
	}

	@JsonIgnore
	public boolean isQuestion() {
		return this instanceof Question;
	}

	@JsonIgnore
	public boolean isFeedback() {
		return this instanceof Feedback;
	}

	public String getPostLinkForRedirect() {
		return getPostLink(false, false, false);
	}

	public String getPostLink(boolean plural, boolean noid) {
		return getPostLink(plural, noid, true);
	}

	public String getPostLink(boolean plural, boolean noid, boolean withContextPathPrefix) {
		Post p = this;
		String ptitle = StringUtils.stripAccents(Utils.noSpaces(Utils.stripAndTrim(p.getTitle()), "-"));
		String pid = (noid ? "" : "/" + Utils.urlEncode(p.getId()) + "/" + Utils.urlEncode(ptitle));
		String ctx = withContextPathPrefix ? CONF.serverContextPath() : "";
		if (p.isQuestion()) {
			return ctx + (plural ? ScooldServer.QUESTIONSLINK : ScooldServer.QUESTIONLINK + pid);
		} else if (p.isFeedback()) {
			return ctx + ScooldServer.FEEDBACKLINK + (plural ? "" : pid);
		} else if (p.isReply()) {
			return ctx + ScooldServer.QUESTIONLINK + (noid ? "" : "/" + p.getParentid());
		}
		return "";
	}

	public void restoreRevisionAndUpdate(String revisionid) {
		Revision rev = client().read(revisionid);
		if (rev != null) {
			//copy rev data to post
			setTitle(rev.getTitle());
			setBody(rev.getBody());
			setTags(rev.getTags());
			setRevisionid(rev.getId());
			setLastactivity(System.currentTimeMillis());
			//update post without creating a new revision
			client().update(this);
			ScooldUtils.getInstance().triggerHookEvent("revision.restore", rev);
		}
	}

	public void addFollower(User user) {
		if (followers == null) {
			followers = new LinkedHashMap<String, String>();
		}
		if (user != null && !StringUtils.isBlank(user.getEmail())) {
			followers.put(user.getId(), user.getEmail());
		}
	}

	public void removeFollower(User user) {
		if (followers != null && user != null) {
			followers.remove(user.getId());
		}
	}

	public boolean hasFollowers() {
		return (followers != null && !followers.isEmpty());
	}

	public boolean hasUpdatedContent(Post beforeUpdate) {
		if (beforeUpdate == null) {
			return false;
		}
		return !StringUtils.equals(getTitle(), beforeUpdate.getTitle())
				|| !StringUtils.equals(getBody(), beforeUpdate.getBody())
				|| !Objects.equals(getTags(), beforeUpdate.getTags());
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getTitle(), ((Post) obj).getTitle())
				&& Objects.equals(getBody(), ((Post) obj).getBody())
				&& Objects.equals(getLocation(), ((Post) obj).getLocation())
				&& Objects.equals(getSpace(), ((Post) obj).getSpace())
				&& Objects.equals(getTags(), ((Post) obj).getTags());
	}

	public int hashCode() {
		return Objects.hashCode(getTitle()) + Objects.hashCode(getBody()) + Objects.hashCode(getTags());
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Feedback extends Post {
	private static final long serialVersionUID = 1L;

	public Feedback() {
		super();
	}

	public enum FeedbackType {
		BUG,
		QUESTION,
		SUGGESTION;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}

}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldServer;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.URL;

public class Profile extends Sysprop {

	private static final long serialVersionUID = 1L;

	@Stored private String originalName;
	@Stored private String originalPicture;
	@Stored private Long lastseen;
	@Stored private String location;
	@Stored private String latlng;
	@Stored private String status;
	@Stored private String aboutme;
	@Stored private String badges;
	@Stored private String groups;
	@Stored private Long upvotes;
	@Stored private Long downvotes;
	@Stored private Long comments;
	@Stored @URL private String picture;
	@Stored @URL private String website;
	@Stored private List<String> favtags;
	@Stored private Set<String> favspaces;
	@Stored private Set<String> modspaces;
	@Stored private Set<String> spaces;
	@Stored private Boolean replyEmailsEnabled;
	@Stored private Boolean commentEmailsEnabled;
	@Stored private Boolean favtagsEmailsEnabled;
	@Stored private Boolean anonymityEnabled;
	@Stored private Boolean darkmodeEnabled;
	@Stored private Integer yearlyVotes;
	@Stored private Integer quarterlyVotes;
	@Stored private Integer monthlyVotes;
	@Stored private Integer weeklyVotes;
	@Stored private List<Map<String, String>> customBadges;
	@Stored private String pendingEmail;
	@Stored private Boolean editorRoleEnabled;
	@Stored private String preferredSpace;

	private transient String currentSpace;
	private transient String newbadges;
	private transient Integer newreports;
	private transient User user;

	public enum Badge {
		VETERAN(10),		//regular visitor		//NOT IMPLEMENTED

		NICEPROFILE(10),	//100% profile completed
		REPORTER(0),		//for every report
		VOTER(0),			//100 total votes
		COMMENTATOR(0),		//100+ comments
		CRITIC(0),			//10+ downvotes
		SUPPORTER(10),		//50+ upvotes
		EDITOR(0),			//first edit of post
		BACKINTIME(0),		//for each rollback of post
		NOOB(10),			//first question + first approved answer
		ENTHUSIAST(0),		//100+ rep  [//			 ]
		FRESHMAN(0),		//300+ rep	[////		 ]
		SCHOLAR(0),			//500+ rep	[//////		 ]
		TEACHER(0),			//1000+ rep	[////////	 ]
		PROFESSOR(0),		//5000+ rep	[//////////	 ]
		GEEK(0),			//9000+ rep	[////////////]
		GOODQUESTION(10),	//20+ votes
		GOODANSWER(10),		//10+ votes
		EUREKA(0),			//for every answer to own question
		SENIOR(0),			//one year + member
		DISCIPLINED(0);		//each time user deletes own comment

		private final int reward;

		Badge(int reward) {
			this.reward = reward;
		}

		public String toString() {
			return super.toString().toLowerCase();
		}

		public Integer getReward() {
			return this.reward;
		}
	}

	public Profile() {
		this(null, null);
	}

	public Profile(String id) {
		this(id, null);
	}

	public Profile(String userid, String name) {
		setId(id(userid));
		setName(name);
		this.status = "";
		this.aboutme = "";
		this.location = "";
		this.website = "";
		this.badges = "";
		this.upvotes = 0L;
		this.downvotes = 0L;
		this.comments = 0L;
		this.yearlyVotes = 0;
		this.quarterlyVotes = 0;
		this.monthlyVotes = 0;
		this.weeklyVotes = 0;
		this.anonymityEnabled = false;
		this.darkmodeEnabled = false;
		this.editorRoleEnabled = true;
		this.favtagsEmailsEnabled = ScooldUtils.getConfig().favoriteTagsEmailsEnabled();
		this.replyEmailsEnabled = ScooldUtils.getConfig().replyEmailsEnabled();
		this.commentEmailsEnabled = ScooldUtils.getConfig().commentEmailsEnabled();
		this.customBadges = new LinkedList<>();
		setTags(new LinkedList<>());
	}

	public static final String id(String userid) {
		if (StringUtils.endsWith(userid, Para.getConfig().separator() + "profile")) {
			return userid;
		} else {
			return userid != null ? userid + Para.getConfig().separator() + "profile" : null;
		}
	}

	public static Profile fromUser(User u) {
		Profile p = new Profile(u.getId(), u.getName());
		p.setUser(u);
		p.setOriginalName(u.getName());
		p.setPicture(u.getPicture());
		p.setAppid(u.getAppid());
		p.setCreatorid(u.getId());
		p.setTimestamp(u.getTimestamp());
		p.setGroups(ScooldUtils.getInstance().isRecognizedAsAdmin(u)
				? User.Groups.ADMINS.toString() : u.getGroups());
		// auto-assign spaces to new users
		ScooldUtils.getInstance().assignSpacesToUser(p, ScooldUtils.getInstance().getAllAutoAssignedSpaces());
		return p;
	}

	private static ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	@JsonIgnore
	public User getUser() {
		if (user == null) {
			user = client().read(getCreatorid() == null
					? StringUtils.removeEnd(getId(), Para.getConfig().separator() + "profile") : getCreatorid());
		}
		return user;
	}

	public Integer getYearlyVotes() {
		if (yearlyVotes < 0) {
			yearlyVotes = 0;
		}
		return yearlyVotes;
	}

	public void setYearlyVotes(Integer yearlyVotes) {
		this.yearlyVotes = yearlyVotes;
	}

	public Integer getQuarterlyVotes() {
		if (quarterlyVotes < 0) {
			quarterlyVotes = 0;
		}
		return quarterlyVotes;
	}

	public void setQuarterlyVotes(Integer quarterlyVotes) {
		this.quarterlyVotes = quarterlyVotes;
	}

	public Integer getMonthlyVotes() {
		if (monthlyVotes < 0) {
			monthlyVotes = 0;
		}
		return monthlyVotes;
	}

	public void setMonthlyVotes(Integer monthlyVotes) {
		this.monthlyVotes = monthlyVotes;
	}

	public Integer getWeeklyVotes() {
		if (weeklyVotes < 0) {
			weeklyVotes = 0;
		}
		return weeklyVotes;
	}

	public void setWeeklyVotes(Integer weeklyVotes) {
		this.weeklyVotes = weeklyVotes;
	}

	public Boolean getReplyEmailsEnabled() {
		return replyEmailsEnabled;
	}

	public void setReplyEmailsEnabled(Boolean replyEmailsEnabled) {
		this.replyEmailsEnabled = replyEmailsEnabled;
	}

	public Boolean getCommentEmailsEnabled() {
		return commentEmailsEnabled;
	}

	public void setCommentEmailsEnabled(Boolean commentEmailsEnabled) {
		this.commentEmailsEnabled = commentEmailsEnabled;
	}

	public Boolean getFavtagsEmailsEnabled() {
		return favtagsEmailsEnabled;
	}

	public void setFavtagsEmailsEnabled(Boolean favtagsEmailsEnabled) {
		this.favtagsEmailsEnabled = favtagsEmailsEnabled;
	}

	public Boolean getAnonymityEnabled() {
		return anonymityEnabled;
	}

	public void setAnonymityEnabled(Boolean anonymityEnabled) {
		this.anonymityEnabled = anonymityEnabled;
	}

	public Boolean getDarkmodeEnabled() {
		return darkmodeEnabled;
	}

	public void setDarkmodeEnabled(Boolean darkmodeEnabled) {
		this.darkmodeEnabled = darkmodeEnabled;
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getCurrentSpace() {
		return currentSpace;
	}

	public void setCurrentSpace(String currentSpace) {
		this.currentSpace = currentSpace;
	}

	public Set<String> getModspaces() {
		if (modspaces == null) {
			modspaces = new LinkedHashSet<>();
		}
		return modspaces;
	}

	public void setModspaces(Set<String> modspaces) {
		this.modspaces = modspaces;
	}

	public String getLatlng() {
		return latlng;
	}

	public void setLatlng(String latlng) {
		this.latlng = latlng;
	}

	public String getNewbadges() {
		return newbadges;
	}

	public void setNewbadges(String newbadges) {
		this.newbadges = newbadges;
	}

	public List<Map<String, String>> getCustomBadges() {
		return customBadges;
	}

	public void setCustomBadges(List<Map<String, String>> customBadges) {
		this.customBadges = customBadges;
	}

	public String getPendingEmail() {
		return pendingEmail;
	}

	public void setPendingEmail(String pendingEmail) {
		this.pendingEmail = pendingEmail;
	}

	public List<String> getFavtags() {
		if (favtags == null) {
			favtags = new LinkedList<String>();
		}
		return favtags;
	}

	public void setFavtags(List<String> favtags) {
		this.favtags = favtags;
	}

	public Set<String> getFavspaces() {
		if (favspaces == null) {
			favspaces = new LinkedHashSet<String>();
		}
		return favspaces;
	}

	public void setFavspaces(Set<String> favspaces) {
		this.favspaces = favspaces;
	}

	public String getPreferredSpace() {
		// returns a preferred staring space upon login
		if (StringUtils.isBlank(preferredSpace)) {
			preferredSpace = ScooldUtils.getConfig().defaultStartingSpace();
		}
		return preferredSpace;
	}

	public void setPreferredSpace(String preferredSpace) {
		this.preferredSpace = preferredSpace;
	}

	public boolean isModInCurrentSpace() {
		return isModInSpace(currentSpace);
	}

	public boolean isModInSpace(String space) {
		return (getModspaces().contains(space) || getModspaces().contains(ScooldUtils.getInstance().getSpaceId(space)));
	}

	public Set<String> getSpaces() {
		if (ScooldUtils.getInstance().isAdmin(this) ||
				(ScooldUtils.getInstance().isMod(this) && ScooldUtils.getConfig().modsAccessAllSpaces())) {
			ScooldUtils utils = ScooldUtils.getInstance();
			spaces = utils.getAllSpaces().stream().
					map(s -> s.getId() + Para.getConfig().separator() + s.getName()).
					sorted((s1, s2) -> utils.getSpaceName(s1).compareToIgnoreCase(utils.getSpaceName(s2))).
					collect(Collectors.toCollection(LinkedHashSet::new));
		}
		if (spaces == null) {
			spaces = new LinkedHashSet<String>();
		}
		String invalidDefault = Post.DEFAULT_SPACE + Para.getConfig().separator() + "default"; // causes problems
		if (spaces.contains(invalidDefault)) {
			spaces.remove(invalidDefault);
			spaces.add(Post.DEFAULT_SPACE);
		}
		if (spaces.isEmpty()) {
			spaces.add(Post.DEFAULT_SPACE);
		}
		// this is confusing - let admins control who is in the default space
		//if (spaces.size() > 1 && spaces.contains(Post.DEFAULT_SPACE)) {
		//	spaces.remove(Post.DEFAULT_SPACE);
		//}
		return spaces;
	}

	public void setSpaces(Set<String> spaces) {
		this.spaces = spaces;
	}

	@JsonIgnore
	public Set<String> getAllSpaces() {
		ScooldUtils utils = ScooldUtils.getInstance();
		return getSpaces().stream().filter(s -> !s.equalsIgnoreCase(Post.DEFAULT_SPACE)).
				sorted((s1, s2) -> utils.getSpaceName(s1).compareToIgnoreCase(utils.getSpaceName(s2))).
				collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public Long getLastseen() {
		return lastseen;
	}

	public void setLastseen(Long val) {
		this.lastseen = val;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public Long getComments() {
		return comments;
	}

	public void setComments(Long comments) {
		this.comments = comments;
	}

	public Long getDownvotes() {
		return downvotes;
	}

	public void setDownvotes(Long downvotes) {
		this.downvotes = downvotes;
	}

	public Long getUpvotes() {
		return upvotes;
	}

	public void setUpvotes(Long upvotes) {
		this.upvotes = upvotes;
	}

	public String getBadges() {
		return badges;
	}

	public void setBadges(String badges) {
		this.badges = badges;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAboutme() {
		return this.aboutme;
	}

	public void setAboutme(String aboutme) {
		this.aboutme = aboutme;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = StringUtils.abbreviate(originalName, 256);
	}

	public String getOriginalPicture() {
		return originalPicture;
	}

	public void setOriginalPicture(String originalPicture) {
		this.originalPicture = originalPicture;
	}

	public Boolean getEditorRoleEnabled() {
		return editorRoleEnabled;
	}

	public void setEditorRoleEnabled(Boolean editorRoleEnabled) {
		this.editorRoleEnabled = editorRoleEnabled;
	}

	public String getFavtagsString() {
		if (getFavtags().isEmpty()) {
			return "";
		}
		return StringUtils.join(getFavtags(), ", ");
	}

	public boolean hasFavtags() {
		return !getFavtags().isEmpty();
	}

	public boolean hasSpaces() {
		return !(getSpaces().size() <= 1 && getSpaces().contains(Post.DEFAULT_SPACE));
	}

	public void removeSpace(String space) {
		String sid = ScooldUtils.getInstance().getSpaceId(space);
		Iterator<String> it = getSpaces().iterator();
		while (it.hasNext()) {
			if (it.next().startsWith(sid + Para.getConfig().separator())) {
				it.remove();
			}
		}
	}

	public long getTotalVotes() {
		if (upvotes == null) {
			upvotes = 0L;
		}
		if (downvotes == null) {
			downvotes = 0L;
		}

		return upvotes + downvotes;
	}

	public void addRep(int rep) {
		if (getVotes() == null) {
			setVotes(0);
		}
		setVotes(getVotes() + rep);
		updateVoteGains(rep);
	}

	public void removeRep(int rep) {
		if (getVotes() == null) {
			setVotes(0);
		}
		setVotes(getVotes() - rep);
		updateVoteGains(-rep);
		if (getVotes() < 0) {
			setVotes(0);
		}
	}

	public void incrementUpvotes() {
		if (this.upvotes == null) {
			this.upvotes = 1L;
		} else {
			this.upvotes = this.upvotes + 1L;
		}
	}

	public void incrementDownvotes() {
		if (this.downvotes == null) {
			this.downvotes = 1L;
		} else {
			this.downvotes = this.downvotes + 1L;
		}
	}

	public void decrementUpvotes() {
		if (this.upvotes == null) {
			this.upvotes = 1L;
		} else {
			this.upvotes = this.upvotes - 1L;
			if (upvotes < 0) {
				upvotes = 0L;
			}
		}
	}

	public void decrementDownvotes() {
		if (this.downvotes == null) {
			this.downvotes = 1L;
		} else {
			this.downvotes = this.downvotes - 1L;
			if (downvotes < 0) {
				downvotes = 0L;
			}
		}
	}

	private void updateVoteGains(int rep) {
		Long updated = Optional.ofNullable(getUpdated()).orElse(getTimestamp());
		LocalDateTime lastUpdate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updated), ZoneId.systemDefault());
		LocalDate now = LocalDate.now();
		if (now.getYear() != lastUpdate.getYear()) {
			yearlyVotes = rep;
		} else {
			yearlyVotes += rep;
		}
		if (now.get(IsoFields.QUARTER_OF_YEAR) != lastUpdate.get(IsoFields.QUARTER_OF_YEAR)) {
			quarterlyVotes = rep;
		} else {
			quarterlyVotes += rep;
		}
		if (now.getMonthValue() != lastUpdate.getMonthValue()) {
			monthlyVotes = rep;
		} else {
			monthlyVotes += rep;
		}
		if (now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) != lastUpdate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)) {
			weeklyVotes = rep;
		} else {
			weeklyVotes += rep;
		}
		setUpdated(Utils.timestamp());
	}

	public boolean hasBadge(Badge b) {
		return StringUtils.containsIgnoreCase(badges, ",".concat(b.toString()).concat(","));
	}

	public void addBadge(Badge b) {
		String badge = b.toString();
		if (StringUtils.isBlank(badges)) {
			badges = ",";
		}
		badges = badges.concat(badge).concat(",");
		addRep(b.getReward());
	}

	public void addBadges(Badge[] larr) {
		for (Badge badge : larr) {
			addBadge(badge);
			addRep(badge.getReward());
		}
	}

	public void removeBadge(Badge b) {
		String badge = b.toString();
		if (StringUtils.isBlank(badges)) {
			return;
		}
		badge = ",".concat(badge).concat(",");

		if (badges.contains(badge)) {
			badges = badges.replaceFirst(badge, ",");
			removeRep(b.getReward());
		}
		if (StringUtils.isBlank(badges.replaceAll(",", ""))) {
			badges = "";
		}
	}

	public HashMap<String, Integer> getBadgesMap() {
		HashMap<String, Integer> badgeMap = new HashMap<String, Integer>(0);
		if (StringUtils.isBlank(badges)) {
			return badgeMap;
		}

		for (String badge : badges.split(",")) {
			Integer val = badgeMap.get(badge);
			int count = (val == null) ? 0 : val.intValue();
			badgeMap.put(badge, ++count);
		}

		badgeMap.remove("");
		return badgeMap;
	}

	public void addCustomBadge(com.erudika.scoold.core.Badge b) {
		if (b != null) {
			if (getTags().size() < 2) {
				Map<String, String> badge = new HashMap<>();
				badge.put("tag", b.getTag());
				badge.put("name", b.getName());
				badge.put("description", b.getDescription());
				badge.put("style", b.getStyle());
				badge.put("icon", b.getIcon());
				customBadges.add(badge);
			}
			getTags().add(b.getTag());
		}
	}

	public boolean removeCustomBadge(String tag) {
		if (!StringUtils.isBlank(tag)) {
			if (getTags().size() <= 2) {
				setCustomBadges(customBadges.stream().filter(b -> !tag.equals(b.get("tag"))).collect(Collectors.toList()));
			}
		}
		return getTags().remove(tag);
	}

	public boolean isComplete() {
		return (!StringUtils.isBlank(location)
				&& !StringUtils.isBlank(aboutme)
				&& !StringUtils.isBlank(website));
	}

	public String create() {
		setLastseen(System.currentTimeMillis());
		client().create(this);
		return getId();
	}

	public void update() {
		setLastseen(System.currentTimeMillis());
		updateVoteGains(0); // reset vote gains if they we're past the time frame
		client().update(this);
	}

	public void delete() {
		client().delete(this);
		client().delete(getUser());
		ScooldUtils.getInstance().unsubscribeFromAllNotifications(this);
	}

	public int countNewReports() {
		if (newreports == null) {
			newreports = client().getCount(Utils.type(Report.class),
					Collections.singletonMap("properties.closed", false)).intValue();
		}
		return newreports;
	}

	public String getProfileLink() {
		String name = StringUtils.stripAccents(Utils.noSpaces(Utils.stripAndTrim(this.getName()), "-"));
		String seoName = StringUtils.isBlank(name) ? "" : ("/" + name);
		String pid = "/" + Utils.urlEncode(this.getCreatorid()) + seoName;
		return ScooldUtils.getConfig().serverContextPath() + ScooldServer.PROFILELINK + pid;
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getName(), ((Profile) obj).getName())
				&& Objects.equals(getLocation(), ((Profile) obj).getLocation())
				&& Objects.equals(getId(), ((Profile) obj).getId());
	}

	public int hashCode() {
		return Objects.hashCode(getName()) + Objects.hashCode(getId());
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.annotations.Stored;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Objects;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Report extends Sysprop {
	private static final long serialVersionUID = 1L;

	@Stored private String subType;
	@Stored private String description;
	@Stored private String authorName;
	@Stored private String link;
	@Stored private String solution;
	@Stored private String content;
	@Stored private Boolean closed;

	public enum ReportType {
		SPAM, OFFENSIVE, DUPLICATE, INCORRECT, OTHER;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	public Report() {
		this(null, null, null, null);
	}

	public Report(String id) {
		this(null, null, null, null);
		setId(id);
	}

	public Report(String parentid, String type, String description, String creatorid) {
		setParentid(parentid);
		setCreatorid(creatorid);
		this.subType = ReportType.OTHER.toString();
		this.description = subType;
		this.closed = false;
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Boolean getClosed() {
		return closed;
	}

	public void setClosed(Boolean closed) {
		this.closed = closed;
	}

	public String getSolution() {
		return solution;
	}

	public void setSolution(String solution) {
		this.solution = solution;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSubType() {
		if (subType == null) {
			subType = ReportType.OTHER.toString();
		}
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public void setSubType(ReportType subType) {
		if (subType != null) {
			this.subType = subType.name();
		}
	}

	public void delete() {
		client().delete(this);
	}

	public void update() {
		client().update(this);
	}

	public String create() {
		Report r = client().create(this);
		if (r != null) {
			ScooldUtils.getInstance().triggerHookEvent("report.create", this);
			setId(r.getId());
			setTimestamp(r.getTimestamp());
			return r.getId();
		}
		return null;
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getSubType(), ((Report) obj).getSubType()) &&
				Objects.equals(getDescription(), ((Report) obj).getDescription()) &&
				Objects.equals(getCreatorid(), ((Report) obj).getCreatorid());
	}

	public int hashCode() {
		return Objects.hashCode(getSubType()) + Objects.hashCode(getDescription()) +
				Objects.hashCode(getCreatorid()) + Objects.hashCode(getParentid());
	}

}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */


import com.erudika.para.core.Sysprop;
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.client.ParaClient;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Revision extends Sysprop {
	private static final long serialVersionUID = 1L;

	@Stored private String body;
	@Stored private String description;
	@Stored private String title;
	@Stored private Boolean original;

	private transient Profile author;

	public Revision() {
		this(null);
	}

	public Revision(String id) {
		setId(id);
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	public Boolean getOriginal() {
		return original;
	}

	public void setOriginal(Boolean original) {
		this.original = original;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		setName(title);
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@JsonIgnore
	public Profile getAuthor() {
		return author;
	}

	public void setAuthor(Profile author) {
		this.author = author;
	}

	public static void createRevisionFromPost(Post post, boolean orig) {
		if (post != null && post.getId() != null) {
			String revUserid = post.getLasteditby();
			if (revUserid == null) {
				revUserid = post.getCreatorid();
			}
			Revision postrev = new Revision();
			postrev.setCreatorid(revUserid);
			postrev.setParentid(post.getId());
			postrev.setTitle(post.getTitle());
			postrev.setBody(post.getBody());
			postrev.setTags(post.getTags());
			postrev.setOriginal(orig);
			String rid = postrev.create();
			if (rid != null) {
				post.setRevisionid(rid);
			}
		}
	}

	public void delete() {
		client().delete(this);
	}

	public void update() {
		client().update(this);
	}

	public String create() {
		Revision r = client().create(this);
		if (r != null) {
			setId(r.getId());
			setTimestamp(r.getTimestamp());
			return r.getId();
		}
		return null;
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getBody(), ((Revision) obj).getBody()) &&
				Objects.equals(getDescription(), ((Revision) obj).getDescription());
	}

	public int hashCode() {
		return Objects.hashCode(getBody()) + Objects.hashCode(getDescription()) + Objects.hashCode(getTimestamp());
	}

}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Reply extends Post {
	private static final long serialVersionUID = 1L;

	public Reply() {
		super();
	}
}


/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class UnapprovedReply extends Reply {
	private static final long serialVersionUID = 1L;
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

/**
 * Sticky post - a question which is pinned to the top of the page.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Sticky extends Post {
	private static final long serialVersionUID = 1L;

	public Sticky() {
		super();
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.Sysprop;
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Badge extends Sysprop {

	private static final long serialVersionUID = 1L;
	private static final String PREFIX = Utils.type(Badge.class).concat(Para.getConfig().separator());

	@Stored private String style;
	@Stored private String icon;
	@Stored private String description;
	@Stored private String tag;

	public Badge() {
		this(null);
	}

	public Badge(String id) {
		if (StringUtils.startsWith(id, PREFIX)) {
			setName(id);
			setTag(id.replaceAll(PREFIX, ""));
			setId(PREFIX.concat(getTag()));
		} else if (id != null) {
			setName(id);
			setTag(id);
			setId(PREFIX.concat(getTag()));
		}
	}

	public String getStyle() {
		return StringEscapeUtils.escapeHtml4(StringUtils.trimToEmpty(style));
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getIcon() {
		if (Utils.isValidURL(icon)) {
			return icon;
		}
		if (StringUtils.trimToEmpty(icon).startsWith(":")) {
			return ":" + StringUtils.substringBetween(icon, ":") + ":";
		}
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = Utils.noSpaces(Utils.stripAndTrim(tag, " "), "-");
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import java.util.LinkedList;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Question extends Post {
	private static final long serialVersionUID = 1L;

	public Question() {
		super();
		setTags(new LinkedList<String>());
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Collections;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Comment extends Sysprop {

	private static final long serialVersionUID = 1L;

	@Stored private String comment;
	@Stored private Boolean hidden;
	@Stored private String authorName;

	public Comment() {
		this(null, null, null);
	}

	public Comment(String creatorid, String comment, String parentid) {
		setCreatorid(creatorid);
		this.comment = comment;
		setParentid(parentid);
		setTimestamp(System.currentTimeMillis()); //now
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String create() {
		if (StringUtils.isBlank(comment) || StringUtils.isBlank(getParentid())) {
			return null;
		}
		int count = client().getCount(getType(), Collections.singletonMap(Config._PARENTID, getParentid())).intValue();
		if (count > ScooldUtils.getConfig().maxCommentsPerPost()) {
			return null;
		}
		this.comment = Utils.abbreviate(this.comment, ScooldUtils.getConfig().maxCommentLength());
		Comment c = client().create(this);
		if (c != null) {
			setId(c.getId());
			setTimestamp(c.getTimestamp());
			return c.getId();
		}
		return null;
	}

	public void update() {
		client().update(this);
	}

	public void delete() {
		client().delete(this);
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getComment(), ((Comment) obj).getComment())
				&& Objects.equals(getCreatorid(), ((Comment) obj).getCreatorid());
	}

	public int hashCode() {
		return Objects.hashCode(getComment()) + Objects.hashCode(getCreatorid());
	}

}

/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Factory that configures a VelocityEngine. Can be used standalone.
 *
 * <p>
 * The optional "configLocation" property sets the location of the Velocity properties file, within the current
 * application. Velocity properties can be overridden via "velocityProperties", or even completely specified locally,
 * avoiding the need for an external properties file.
 *
 * <p>
 * The "resourceLoaderPath" property can be used to specify the Velocity resource loader path via Spring's Resource
 * abstraction, possibly relative to the Spring application context.
 *
 * <p>
 * The simplest way to use this class is to specify a {@link #setResourceLoaderPath(String) "resourceLoaderPath"}; the
 * VelocityEngine typically then does not need any further configuration.
 *
 * @author Juergen Hoeller
 */
public class VelocityEngineFactory {

	private static final Logger logger = LoggerFactory.getLogger(VelocityEngineFactory.class);

	private Resource configLocation;

	private final Map<String, Object> velocityProperties = new HashMap<String, Object>();

	private String resourceLoaderPath;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private boolean preferFileSystemAccess = true;

	/**
	 * Set the location of the Velocity config file. Alternatively, you can specify all properties locally.
	 *
	 * @see #setVelocityProperties
	 * @see #setResourceLoaderPath
	 * @param configLocation config
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * Set Velocity properties, like "file.resource.loader.path". Can be used to override values in a Velocity config
	 * file, or to specify all necessary properties locally.
	 * <p>
	 * Note that the Velocity resource loader path also be set to any Spring resource location via the
	 * "resourceLoaderPath" property. Setting it here is just necessary when using a non-file-based resource loader.
	 *
	 * @see #setVelocityPropertiesMap
	 * @see #setConfigLocation
	 * @see #setResourceLoaderPath
	 * @param velocityProperties props
	 */
	public void setVelocityProperties(Properties velocityProperties) {
		CollectionUtils.mergePropertiesIntoMap(velocityProperties, this.velocityProperties);
	}

	/**
	 * Set Velocity properties as Map, to allow for non-String values like "ds.resource.loader.instance".
	 *
	 * @see #setVelocityProperties
	 * @param velocityPropertiesMap map
	 */
	public void setVelocityPropertiesMap(Map<String, Object> velocityPropertiesMap) {
		if (velocityPropertiesMap != null) {
			this.velocityProperties.putAll(velocityPropertiesMap);
		}
	}

	/**
	 * Set the Velocity resource loader path via a Spring resource location. Accepts multiple locations in Velocity's
	 * comma-separated path style.
	 * <p>
	 * When populated via a String, standard URLs like "file:" and "classpath:" pseudo URLs are supported, as understood
	 * by ResourceLoader. Allows for relative paths when running in an ApplicationContext.
	 * <p>
	 * Will define a path for the default Velocity resource loader with the name "file". If the specified resource
	 * cannot be resolved to a {@code java.io.File}, a generic SpringResourceLoader will be used under the name
	 * "spring", without modification detection.
	 * <p>
	 * Note that resource caching will be enabled in any case. With the file resource loader, the last-modified
	 * timestamp will be checked on access to detect changes. With SpringResourceLoader, the resource will be cached
	 * forever (for example for class path resources).
	 * <p>
	 * To specify a modification check interval for files, use Velocity's standard
	 * "file.resource.loader.modificationCheckInterval" property. By default, the file timestamp is checked on every
	 * access (which is surprisingly fast). Of course, this just applies when loading resources from the file system.
	 * <p>
	 * To enforce the use of SpringResourceLoader, i.e. to not resolve a path as file system resource in any case, turn
	 * off the "preferFileSystemAccess" flag. See the latter's javadoc for details.
	 *
	 * @see #setVelocityProperties
	 * @param resourceLoaderPath path
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	/**
	 * Set the Spring ResourceLoader to use for loading Velocity template files. The default is DefaultResourceLoader.
	 * Will get overridden by the ApplicationContext if running in a context.
	 *
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.context.ApplicationContext
	 * @param resourceLoader loader
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Return the Spring ResourceLoader to use for loading Velocity template files.
	 * @return loader
	 */
	protected ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * Set whether to prefer file system access for template loading. File system access enables hot detection of
	 * template changes.
	 * <p>
	 * If this is enabled, VelocityEngineFactory will try to resolve the specified "resourceLoaderPath" as file system
	 * resource (which will work for expanded class path resources and ServletContext resources too).
	 * <p>
	 * Default is "true". Turn this off to always load via SpringResourceLoader (i.e. as stream, without hot detection
	 * of template changes), which might be necessary if some of your templates reside in an expanded classes directory
	 * while others reside in jar files.
	 *
	 * @see #setResourceLoaderPath
	 * @param preferFileSystemAccess bool
	 */
	public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
		this.preferFileSystemAccess = preferFileSystemAccess;
	}

	/**
	 * Return whether to prefer file system access for template loading.
	 * @return bool
	 */
	protected boolean isPreferFileSystemAccess() {
		return this.preferFileSystemAccess;
	}

	/**
	 * Prepare the VelocityEngine instance and return it.
	 *
	 * @return the VelocityEngine instance
	 * @throws IOException if the config file wasn't found
	 * @throws VelocityException on Velocity initialization failure
	 */
	public VelocityEngine createVelocityEngine() throws IOException, VelocityException {
		VelocityEngine velocityEngine = newVelocityEngine();
		Map<String, Object> props = new HashMap<String, Object>();

		// Load config file if set.
		if (this.configLocation != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Loading Velocity config from [" + this.configLocation + "]");
			}
			CollectionUtils.mergePropertiesIntoMap(PropertiesLoaderUtils.loadProperties(this.configLocation), props);
		}

		// Merge local properties if set.
		if (!this.velocityProperties.isEmpty()) {
			props.putAll(this.velocityProperties);
		}

		// Set a resource loader path, if required.
		if (this.resourceLoaderPath != null) {
			initVelocityResourceLoader(velocityEngine, this.resourceLoaderPath);
		}

		// Apply properties to VelocityEngine.
		for (Map.Entry<String, Object> entry : props.entrySet()) {
			velocityEngine.setProperty(entry.getKey(), entry.getValue());
		}

		postProcessVelocityEngine(velocityEngine);

		// Perform actual initialization.
		velocityEngine.init();

		return velocityEngine;
	}

	/**
	 * Return a new VelocityEngine. Subclasses can override this for custom initialization, or for using a mock object
	 * for testing.
	 * <p>
	 * Called by {@code createVelocityEngine()}.
	 *
	 * @return the VelocityEngine instance
	 * @throws IOException if a config file wasn't found
	 * @throws VelocityException on Velocity initialization failure
	 * @see #createVelocityEngine()
	 */
	protected VelocityEngine newVelocityEngine() throws IOException, VelocityException {
		return new VelocityEngine();
	}

	/**
	 * Initialize a Velocity resource loader for the given VelocityEngine: either a standard Velocity FileResourceLoader
	 * or a SpringResourceLoader.
	 * <p>
	 * Called by {@code createVelocityEngine()}.
	 *
	 * @param velocityEngine the VelocityEngine to configure
	 * @param resourceLoaderPath the path to load Velocity resources from
	 * @see #createVelocityEngine()
	 */
	protected void initVelocityResourceLoader(VelocityEngine velocityEngine, String resourceLoaderPath) {
		if (isPreferFileSystemAccess()) {
			// Try to load via the file system, fall back to SpringResourceLoader
			// (for hot detection of template changes, if possible).
			try {
				StringBuilder resolvedPath = new StringBuilder();
				String[] paths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
				for (int i = 0; i < paths.length; i++) {
					String path = paths[i];
					Resource resource = getResourceLoader().getResource(path);
					File file = resource.getFile();  // will fail if not resolvable in the file system
					if (logger.isDebugEnabled()) {
						logger.debug("Resource loader path [" + path + "] resolved to file [" + file.getAbsolutePath() + "]");
					}
					resolvedPath.append(file.getAbsolutePath());
					if (i < paths.length - 1) {
						resolvedPath.append(',');
					}
				}
				velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
				velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
				velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, resolvedPath.toString());
			} catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot resolve resource loader path [" + resourceLoaderPath
							+ "] to [java.io.File]: using SpringResourceLoader", ex);
				}
				initSpringResourceLoader(velocityEngine, resourceLoaderPath);
			}
		} else {
			// Always load via SpringResourceLoader
			// (without hot detection of template changes).
			if (logger.isDebugEnabled()) {
				logger.debug("File system access not preferred: using SpringResourceLoader");
			}
			initSpringResourceLoader(velocityEngine, resourceLoaderPath);
		}
	}

	/**
	 * Initialize a SpringResourceLoader for the given VelocityEngine.
	 * <p>
	 * Called by {@code initVelocityResourceLoader}.
	 *
	 * @param velocityEngine the VelocityEngine to configure
	 * @param resourceLoaderPath the path to load Velocity resources from
	 * @see SpringResourceLoader
	 * @see #initVelocityResourceLoader
	 */
	protected void initSpringResourceLoader(VelocityEngine velocityEngine, String resourceLoaderPath) {
		velocityEngine.setProperty(
				RuntimeConstants.RESOURCE_LOADERS, SpringResourceLoader.NAME);
		velocityEngine.setProperty(
				SpringResourceLoader.SPRING_RESOURCE_LOADER_CLASS, SpringResourceLoader.class.getName());
		velocityEngine.setProperty(
				SpringResourceLoader.SPRING_RESOURCE_LOADER_CACHE, "true");
		velocityEngine.setApplicationAttribute(
				SpringResourceLoader.SPRING_RESOURCE_LOADER, getResourceLoader());
		velocityEngine.setApplicationAttribute(
				SpringResourceLoader.SPRING_RESOURCE_LOADER_PATH, resourceLoaderPath);
	}


	/**
	 * To be implemented by subclasses that want to perform custom post-processing of the VelocityEngine after this
	 * FactoryBean performed its default configuration (but before VelocityEngine.init).
	 * <p>
	 * Called by {@code createVelocityEngine()}.
	 *
	 * @param velocityEngine the current VelocityEngine
	 * @throws IOException if a config file wasn't found
	 * @throws VelocityException on Velocity initialization failure
	 * @see #createVelocityEngine()
	 * @see org.apache.velocity.app.VelocityEngine#init
	 */
	protected void postProcessVelocityEngine(VelocityEngine velocityEngine)
			throws IOException, VelocityException {
	}

}

/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * <p>
 * The view class for all views generated by this resolver can be specified via the "viewClass" property. See
 * UrlBasedViewResolver's javadoc for details.
 *
 * <p>
 * <b>Note:</b> When chaining ViewResolvers, a VelocityViewResolver will check for the existence of the specified
 * template resources and only return a non-null View object if the template was actually found.
 *
 * @author Juergen Hoeller
 * @since 13.12.2003
 */
public class VelocityViewResolver extends AbstractTemplateViewResolver {

	public VelocityViewResolver() {
		setViewClass(requiredViewClass());
	}

	@Override
	protected Class<?> requiredViewClass() {
		return VelocityView.class;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		VelocityView view = (VelocityView) super.buildView(viewName);
		return view;
	}

}

/*
 * Copyright 2002-2012 the original author or authors.
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


import org.apache.velocity.app.VelocityEngine;

/**
 * Interface to be implemented by objects that configure and manage a
 * VelocityEngine for automatic lookup in a web environment. Detected
 * and used by VelocityView.
 *
 * @author Rod Johnson
 * @see VelocityConfigurer
 * @see VelocityView
 */
public interface VelocityConfig {

	/**
	 * Return the VelocityEngine for the current web application context.
	 * May be unique to one servlet, or shared in the root context.
	 * @return the VelocityEngine
	 */
	VelocityEngine getVelocityEngine();

}

/*
 * Copyright 2002-2012 the original author or authors.
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


import java.io.IOException;
import jakarta.servlet.ServletContext;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.web.context.ServletContextAware;

/**
 * JavaBean to configure Velocity for web usage, via the "configLocation"
 * and/or "velocityProperties" and/or "resourceLoaderPath" bean properties.
 * The simplest way to use this class is to specify just a "resourceLoaderPath";
 * you do not need any further configuration then.
 * *
 * This bean must be included in the application context of any application
 * using Spring's {@link VelocityView} for web MVC. It exists purely to configure
 * Velocity; it is not meant to be referenced by application components (just
 * internally by VelocityView). This class implements {@link VelocityConfig}
 * in order to be found by VelocityView without depending on the bean name of
 * this configurer. Each DispatcherServlet may define its own VelocityConfigurer
 * if desired, potentially with different template loader paths.
 *
 * <p>Note that you can also refer to a pre-configured VelocityEngine
 * instance via the "velocityEngine" property.
 * This allows to share a VelocityEngine for web and email usage, for example.
 *
 * <p>This configurer registers the "spring.vm" Velocimacro library for web views
 * (contained in this package and thus in {@code spring.jar}), which makes
 * all of Spring's default Velocity macros available to the views.
 * This allows for using the Spring-provided macros such as follows:
 *
 * <pre class="code">
 * #springBind("person.age")
 * age is ${status.value}</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Darren Davison
 * @see #setConfigLocation
 * @see #setVelocityProperties
 * @see #setResourceLoaderPath
 * @see #setVelocityEngine
 * @see VelocityView
 */
public class VelocityConfigurer extends VelocityEngineFactory
		implements VelocityConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {

	private VelocityEngine velocityEngine;

	private ServletContext servletContext;


	/**
	 * Set a pre-configured VelocityEngine to use for the Velocity web
	 * configuration.
	 * <p>Note that the Spring macros will <i>not</i> be enabled automatically in
	 * case of an external VelocityEngine passed in here. Make sure to include
	 * {@code spring.vm} in your template loader path in such a scenario
	 * (if there is an actual need to use those macros).
	 * <p>If this is not set, VelocityEngineFactory's properties
	 * (inherited by this class) have to be specified.
	 * @param velocityEngine engine
	 */
	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * Initialize VelocityEngineFactory's VelocityEngine
	 * if not overridden by a pre-configured VelocityEngine.
	 * @see #createVelocityEngine
	 * @see #setVelocityEngine
	 */
	@Override
	public void afterPropertiesSet() throws IOException, VelocityException {
		if (this.velocityEngine == null) {
			this.velocityEngine = createVelocityEngine();
		}
	}

	/**
	 * Provides a ClasspathResourceLoader in addition to any default or user-defined
	 * loader in order to load the spring Velocity macros from the class path.
	 * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
	 * @param velocityEngine engine
	 */
	@Override
	protected void postProcessVelocityEngine(VelocityEngine velocityEngine) {
		velocityEngine.setApplicationAttribute(ServletContext.class.getName(), this.servletContext);
	}

	@Override
	public VelocityEngine getVelocityEngine() {
		return this.velocityEngine;
	}

}

/*
 * Copyright 2002-2012 the original author or authors.
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


import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * Convenience subclass of VelocityViewResolver, adding support
 * for VelocityLayoutView and its properties.
 *
 * <p>See VelocityViewResolver's javadoc for general usage info.
 *
 * @author Juergen Hoeller
 * @since 1.2.7
 * @see VelocityViewResolver
 * @see VelocityLayoutView
 * @see #setLayoutUrl
 * @see #setLayoutKey
 * @see #setScreenContentKey
 */
public class VelocityLayoutViewResolver extends VelocityViewResolver {

	private String layoutUrl;

	private String layoutKey;

	private String screenContentKey;


	/**
	 * Requires VelocityLayoutView.
	 * @see VelocityLayoutView
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return VelocityLayoutView.class;
	}

	/**
	 * Set the layout template to use. Default is "layout.vm".
	 * @param layoutUrl the template location (relative to the template
	 * root directory)
	 * @see VelocityLayoutView#setLayoutUrl
	 */
	public void setLayoutUrl(String layoutUrl) {
		this.layoutUrl = layoutUrl;
	}

	/**
	 * Set the context key used to specify an alternate layout to be used instead
	 * of the default layout. Screen content templates can override the layout
	 * template that they wish to be wrapped with by setting this value in the
	 * template, for example:<br>
	 * {@code #set($layout = "MyLayout.vm" )}
	 * <p>The default key is "layout", as illustrated above.
	 * @param layoutKey the name of the key you wish to use in your
	 * screen content templates to override the layout template
	 * @see VelocityLayoutView#setLayoutKey
	 */
	public void setLayoutKey(String layoutKey) {
		this.layoutKey = layoutKey;
	}

	/**
	 * Set the name of the context key that will hold the content of
	 * the screen within the layout template. This key must be present
	 * in the layout template for the current screen to be rendered.
	 * <p>Default is "screen_content": accessed in VTL as
	 * {@code $screen_content}.
	 * @param screenContentKey the name of the screen content key to use
	 * @see VelocityLayoutView#setScreenContentKey
	 */
	public void setScreenContentKey(String screenContentKey) {
		this.screenContentKey = screenContentKey;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		VelocityLayoutView view = (VelocityLayoutView) super.buildView(viewName);
		// Use not-null checks to preserve VelocityLayoutView's defaults.
		if (this.layoutUrl != null) {
			view.setLayoutUrl(this.layoutUrl);
		}
		if (this.layoutKey != null) {
			view.setLayoutKey(this.layoutKey);
		}
		if (this.screenContentKey != null) {
			view.setScreenContentKey(this.screenContentKey);
		}
		return view;
	}

}

/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * VelocityLayoutView emulates the functionality offered by Velocity's VelocityLayoutServlet to ease page composition
 * from different templates.
 *
 * <p>
 * The {@code url} property should be set to the content template for the view, and the layout template location should
 * be specified as {@code layoutUrl} property. A view can override the configured layout template location by setting
 * the appropriate key (the default is "layout") in the content template.
 *
 * <p>
 * When the view is rendered, the VelocityContext is first merged with the content template (specified by the
 * {@code url} property) and then merged with the layout template to produce the final output.
 *
 * <p>
 * The layout template can include the screen content through a VelocityContext variable (the default is
 * "screen_content"). At runtime, this variable will contain the rendered content template.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setLayoutUrl
 * @see #setLayoutKey
 * @see #setScreenContentKey
 */
public class VelocityLayoutView extends VelocityView {

	/**
	 * The default {@link #setLayoutUrl(String) layout url}.
	 */
	public static final String DEFAULT_LAYOUT_URL = "layout.vm";

	/**
	 * The default {@link #setLayoutKey(String) layout key}.
	 */
	public static final String DEFAULT_LAYOUT_KEY = "layout";

	/**
	 * The default {@link #setScreenContentKey(String) screen content key}.
	 */
	public static final String DEFAULT_SCREEN_CONTENT_KEY = "screen_content";

	private String layoutUrl = DEFAULT_LAYOUT_URL;

	private String layoutKey = DEFAULT_LAYOUT_KEY;

	private String screenContentKey = DEFAULT_SCREEN_CONTENT_KEY;

	/**
	 * Set the layout template to use. Default is {@link #DEFAULT_LAYOUT_URL "layout.vm"}.
	 *
	 * @param layoutUrl the template location (relative to the template root directory)
	 */
	public void setLayoutUrl(String layoutUrl) {
		this.layoutUrl = layoutUrl;
	}

	/**
	 * Set the context key used to specify an alternate layout to be used instead of the default layout. Screen content
	 * templates can override the layout template that they wish to be wrapped with by setting this value in the
	 * template, for example:<br> {@code #set($layout = "MyLayout.vm" )}
	 * <p>
	 * Default key is {@link #DEFAULT_LAYOUT_KEY "layout"}, as illustrated above.
	 *
	 * @param layoutKey the name of the key you wish to use in your screen content templates to override the layout
	 * template
	 */
	public void setLayoutKey(String layoutKey) {
		this.layoutKey = layoutKey;
	}

	/**
	 * Set the name of the context key that will hold the content of the screen within the layout template. This key
	 * must be present in the layout template for the current screen to be rendered.
	 * <p>
	 * Default is {@link #DEFAULT_SCREEN_CONTENT_KEY "screen_content"}: accessed in VTL as {@code $screen_content}.
	 *
	 * @param screenContentKey the name of the screen content key to use
	 */
	public void setScreenContentKey(String screenContentKey) {
		this.screenContentKey = screenContentKey;
	}

	/**
	 * Overrides {@code VelocityView.checkTemplate()} to additionally check that both the layout template and the screen
	 * content template can be loaded. Note that during rendering of the screen content, the layout template can be
	 * changed which may invalidate any early checking done here.
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		if (!super.checkResource(locale)) {
			return false;
		}

		try {
			// Check that we can get the template, even if we might subsequently get it again.
			getTemplate(this.layoutUrl);
			return true;
		} catch (ResourceNotFoundException ex) {
			throw new IOException("Cannot find Velocity template for URL [" + this.layoutUrl
					+ "]: Did you specify the correct resource loader path?", ex);
		} catch (Exception ex) {
			throw new IOException(
					"Could not load Velocity template for URL [" + this.layoutUrl + "]", ex);
		}
	}

	/**
	 * Overrides the normal rendering process in order to pre-process the Context, merging it with the screen template
	 * into a single value (identified by the value of screenContentKey). The layout template is then merged with the
	 * modified Context in the super class.
	 */
	@Override
	protected void doRender(Context context, HttpServletResponse response) throws Exception {
		renderScreenContent(context);

		// Velocity context now includes any mappings that were defined
		// (via #set) in screen content template.
		// The screen template can overrule the layout by doing
		// #set( $layout = "MyLayout.vm" )
		String layoutUrlToUse = (String) context.get(this.layoutKey);
		if (layoutUrlToUse != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Screen content template has requested layout [" + layoutUrlToUse + "]");
			}
		} else {
			// No explicit layout URL given -> use default layout of this view.
			layoutUrlToUse = this.layoutUrl;
		}

		mergeTemplate(getTemplate(layoutUrlToUse), context, response);
	}

	/**
	 * The resulting context contains any mappings from render, plus screen content.
	 */
	private void renderScreenContent(Context velocityContext) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering screen content template [" + getUrl() + "]");
		}

		StringWriter sw = new StringWriter();
		Template screenContentTemplate = getTemplate(getUrl());
		screenContentTemplate.merge(velocityContext, sw);

		// Put rendered content into Velocity context.
		velocityContext.put(this.screenContentKey, sw.toString());
	}

}

/*
 * Copyright 2002-2012 the original author or authors.
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


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

/**
 * Velocity ResourceLoader adapter that loads via a Spring ResourceLoader.
 * Used by VelocityEngineFactory for any resource loader path that cannot
 * be resolved to a {@code java.io.File}.
 *
 * <p>Note that this loader does not allow for modification detection:
 * Use Velocity's default FileResourceLoader for {@code java.io.File}
 * resources.
 *
 * <p>Expects "spring.resource.loader" and "spring.resource.loader.path"
 * application attributes in the Velocity runtime: the former of type
 * {@code org.springframework.core.io.ResourceLoader}, the latter a String.
 *
 * @author Juergen Hoeller
 * @since 14.03.2004
 */
public class SpringResourceLoader extends ResourceLoader {

	public static final String NAME = "spring";

	public static final String SPRING_RESOURCE_LOADER_CLASS = "resource.loader.spring.class";

	public static final String SPRING_RESOURCE_LOADER_CACHE = "resource.loader.spring.cache";

	public static final String SPRING_RESOURCE_LOADER = "spring.resource.loader";

	public static final String SPRING_RESOURCE_LOADER_PATH = "spring.resource.loader.path";


	private static final Logger logger = LoggerFactory.getLogger(SpringResourceLoader.class);

	private org.springframework.core.io.ResourceLoader resourceLoader;

	private String[] resourceLoaderPaths;


	@Override
	public void init(ExtProperties configuration) {
		this.resourceLoader = (org.springframework.core.io.ResourceLoader)
				this.rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER);
		String resourceLoaderPath = (String) this.rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER_PATH);
		if (this.resourceLoader == null) {
			throw new IllegalArgumentException(
					"'resourceLoader' application attribute must be present for SpringResourceLoader");
		}
		if (resourceLoaderPath == null) {
			throw new IllegalArgumentException(
					"'resourceLoaderPath' application attribute must be present for SpringResourceLoader");
		}
		this.resourceLoaderPaths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
		for (int i = 0; i < this.resourceLoaderPaths.length; i++) {
			String path = this.resourceLoaderPaths[i];
			if (!path.endsWith("/")) {
				this.resourceLoaderPaths[i] = path + "/";
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("SpringResourceLoader for Velocity: using resource loader [" + this.resourceLoader +
					"] and resource loader paths " + Arrays.asList(this.resourceLoaderPaths));
		}
	}

	@Override
	public Reader getResourceReader(String source, String encoding) throws ResourceNotFoundException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for Velocity resource with name [" + source + "]");
		}
		for (String resourceLoaderPath : this.resourceLoaderPaths) {
			org.springframework.core.io.Resource resource =
					this.resourceLoader.getResource(resourceLoaderPath + source);
			try {
				return new InputStreamReader(resource.getInputStream());
			} catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find Velocity resource: " + resource);
				}
			}
		}
		throw new ResourceNotFoundException(
				"Could not find resource [" + source + "] in Spring resource loader path");
	}

	@Override
	public boolean isSourceModified(Resource resource) {
		return false;
	}

	@Override
	public long getLastModified(Resource resource) {
		return 0;
	}

}

/*
 * Copyright 2002-2013 the original author or authors.
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

import com.erudika.para.core.utils.Para;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * View using the Velocity template engine.
 *
 * <p>
 * Exposes the following JavaBean properties:
 * <ul>
 * <li><b>url</b>: the location of the Velocity template to be wrapped, relative to the Velocity resource loader path
 * (see VelocityConfigurer).
 * <li><b>encoding</b> (optional, default is determined by Velocity configuration): the encoding of the Velocity
 * template file
 * <li><b>velocityFormatterAttribute</b> (optional, default=null): the name of the VelocityFormatter helper object to
 * expose in the Velocity context of this view, or {@code null} if not needed. VelocityFormatter is part of standard
 * Velocity.
 * <li><b>dateToolAttribute</b> (optional, default=null): the name of the DateTool helper object to expose in the
 * Velocity context of this view, or {@code null} if not needed. DateTool is part of Velocity Tools.
 * <li><b>numberToolAttribute</b> (optional, default=null): the name of the NumberTool helper object to expose in the
 * Velocity context of this view, or {@code null} if not needed. NumberTool is part of Velocity Tools.
 * <li><b>cacheTemplate</b> (optional, default=false): whether or not the Velocity template should be cached. It should
 * normally be true in production, but setting this to false enables us to modify Velocity templates without restarting
 * the application (similar to JSPs). Note that this is a minor optimization only, as Velocity itself caches templates
 * in a modification-aware fashion.
 * </ul>
 *
 * <p>
 * Depends on a VelocityConfig object such as VelocityConfigurer being accessible in the current web application
 * context, with any bean name. Alternatively, you can set the VelocityEngine object as bean property.
 *
 * <p>
 * Note: Spring 3.0's VelocityView requires Velocity 1.4 or higher, and optionally Velocity Tools 1.1 or higher
 * (depending on the use of DateTool and/or NumberTool).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 * @see VelocityConfig
 * @see VelocityConfigurer
 */
public class VelocityView extends AbstractTemplateView {

	private Map<String, Class<?>> toolAttributes;

	private boolean cacheTemplate = false;

	private VelocityEngine velocityEngine;

	private Template template;

	/**
	 * Set whether the Velocity template should be cached. Default is "false". It should normally be true in production,
	 * but setting this to false enables us to modify Velocity templates without restarting the application (similar to
	 * JSPs).
	 * <p>
	 * Note that this is a minor optimization only, as Velocity itself caches templates in a modification-aware fashion.
	 * @param cacheTemplate cache
	 */
	public void setCacheTemplate(boolean cacheTemplate) {
		this.cacheTemplate = cacheTemplate;
	}

	/**
	 * Return whether the Velocity template should be cached.
	 * @return bool
	 */
	protected boolean isCacheTemplate() {
		return this.cacheTemplate;
	}

	/**
	 * Set the VelocityEngine to be used by this view.
	 * <p>
	 * If this is not set, the default lookup will occur: A single VelocityConfig is expected in the current web
	 * application context, with any bean name.
	 *
	 * @see VelocityConfig
	 * @param velocityEngine engine
	 */
	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	/**
	 * Return the VelocityEngine used by this view.
	 * @return engine
	 */
	protected VelocityEngine getVelocityEngine() {
		return this.velocityEngine;
	}

	/**
	 * Invoked on startup. Looks for a single VelocityConfig bean to find the relevant VelocityEngine for this factory.
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		super.initApplicationContext();

		if (getVelocityEngine() == null) {
			// No explicit VelocityEngine: try to autodetect one.
			setVelocityEngine(autodetectVelocityEngine());
		}
	}

	/**
	 * Autodetect a VelocityEngine via the ApplicationContext. Called if no explicit VelocityEngine has been specified.
	 *
	 * @return the VelocityEngine to use for VelocityViews
	 * @throws BeansException if no VelocityEngine could be found
	 * @see #getApplicationContext
	 * @see #setVelocityEngine
	 */
	protected VelocityEngine autodetectVelocityEngine() throws BeansException {
		try {
			VelocityConfig velocityConfig = BeanFactoryUtils.beanOfTypeIncludingAncestors(
					getApplicationContext(), VelocityConfig.class, true, false);
			return velocityConfig.getVelocityEngine();
		} catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single VelocityConfig bean in this web application context "
					+ "(may be inherited): VelocityConfigurer is the usual implementation. "
					+ "This bean may be given any name.", ex);
		}
	}

	/**
	 * Check that the Velocity template used for this view exists and is valid.
	 * <p>
	 * Can be overridden to customize the behavior, for example in case of multiple templates to be rendered into a
	 * single view.
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		try {
			// Check that we can get the template, even if we might subsequently get it again.
			this.template = getTemplate(getUrl());
			return true;
		} catch (ResourceNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No Velocity view found for URL: " + getUrl());
			}
			return false;
		} catch (Exception ex) {
			throw new IOException(
					"Could not load Velocity template for URL [" + getUrl() + "]", ex);
		}
	}

	/**
	 * Process the model map by merging it with the Velocity template. Output is directed to the servlet response.
	 * <p>
	 * This method can be overridden if custom behavior is needed.
	 */
	@Override
	protected void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		exposeHelpers(model, request);

		Context velocityContext = createVelocityContext(model, request, response);
		exposeHelpers(velocityContext, request, response);
		exposeToolAttributes(velocityContext, request);

		doRender(velocityContext, response);
	}

	/**
	 * Expose helpers unique to each rendering operation. This is necessary so that different rendering operations can't
	 * overwrite each other's formats etc.
	 * <p>
	 * Called by {@code renderMergedTemplateModel}. The default implementation is empty. This method can be overridden
	 * to add custom helpers to the model.
	 *
	 * @param model the model that will be passed to the template for merging
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding model attributes
	 * @see #renderMergedTemplateModel
	 */
	protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
	}

	/**
	 * Create a Velocity Context instance for the given model, to be passed to the template for merging.
	 * <p>
	 * The default implementation delegates to {@link #createVelocityContext(Map)}. Can be overridden for a special
	 * context class, for example ChainedContext which is part of the view package of Velocity Tools. ChainedContext is
	 * needed for initialization of ViewTool instances.
	 * <p>
	 * Have a look at {@link VelocityView}, which pre-implements ChainedContext support. This is not part of the
	 * standard VelocityView class in order to avoid a required dependency on the view package of Velocity Tools.
	 *
	 * @param model the model Map, containing the model attributes to be exposed to the view
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return the Velocity Context
	 * @throws Exception if there's a fatal error while creating the context
	 * @see VelocityView
	 */
	protected Context createVelocityContext(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		return createVelocityContext(model);
	}

	/**
	 * Create a Velocity Context instance for the given model, to be passed to the template for merging.
	 * <p>
	 * Default implementation creates an instance of Velocity's VelocityContext implementation class.
	 *
	 * @param model the model Map, containing the model attributes to be exposed to the view
	 * @return the Velocity Context
	 * @throws Exception if there's a fatal error while creating the context
	 * @see org.apache.velocity.VelocityContext
	 */
	protected Context createVelocityContext(Map<String, Object> model) throws Exception {
		return new VelocityContext(model);
	}

	/**
	 * Expose helpers unique to each rendering operation. This is necessary so that different rendering operations can't
	 * overwrite each other's formats etc.
	 * <p>
	 * Called by {@code renderMergedTemplateModel}. Default implementation delegates to
	 * {@code exposeHelpers(velocityContext, request)}. This method can be overridden to add special tools to the
	 * context, needing the servlet response to initialize (see Velocity Tools, for example LinkTool and
	 * ViewTool/ChainedContext).
	 *
	 * @param velocityContext Velocity context that will be passed to the template
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if there's a fatal error while we're adding model attributes
	 * @see #exposeHelpers(org.apache.velocity.context.Context, HttpServletRequest)
	 */
	protected void exposeHelpers(
			Context velocityContext, HttpServletRequest request, HttpServletResponse response) throws Exception {

		exposeHelpers(velocityContext, request);
	}

	/**
	 * Expose helpers unique to each rendering operation. This is necessary so that different rendering operations can't
	 * overwrite each other's formats etc.
	 * <p>
	 * Default implementation is empty. This method can be overridden to add custom helpers to the Velocity context.
	 *
	 * @param velocityContext Velocity context that will be passed to the template
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding model attributes
	 * @see #exposeHelpers(Map, HttpServletRequest)
	 */
	protected void exposeHelpers(Context velocityContext, HttpServletRequest request) throws Exception {
	}

	/**
	 * Expose the tool attributes, according to corresponding bean property settings.
	 * <p>
	 * Do not override this method unless for further tools driven by bean properties. Override one of the
	 * {@code exposeHelpers} methods to add custom helpers.
	 *
	 * @param velocityContext Velocity context that will be passed to the template
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding model attributes
	 */
	protected void exposeToolAttributes(Context velocityContext, HttpServletRequest request) throws Exception {
		// Expose generic attributes.
		if (this.toolAttributes != null) {
			for (Map.Entry<String, Class<?>> entry : this.toolAttributes.entrySet()) {
				String attributeName = entry.getKey();
				Class<?> toolClass = entry.getValue();
				try {
					Object tool = toolClass.getDeclaredConstructor().newInstance();
					initTool(tool, velocityContext);
					velocityContext.put(attributeName, tool);
				} catch (Exception ex) {
					throw new ServletException("Could not instantiate Velocity tool '" + attributeName + "'", ex);
				}
			}
		}
	}

	/**
	 * Initialize the given tool instance. The default implementation is empty.
	 * <p>
	 * Can be overridden to check for special callback interfaces, for example the ViewContext interface which is part
	 * of the view package of Velocity Tools. In the particular case of ViewContext, you'll usually also need a special
	 * Velocity context, like ChainedContext which is part of Velocity Tools too.
	 * <p>
	 * Have a look at {@link VelocityView}, which pre-implements such a ViewTool check. This is not part of the
	 * standard VelocityView class in order to avoid a required dependency on the view package of Velocity Tools.
	 *
	 * @param tool the tool instance to initialize
	 * @param velocityContext the Velocity context
	 * @throws Exception if initializion of the tool failed
	 */
	protected void initTool(Object tool, Context velocityContext) throws Exception {
	}

	/**
	 * Render the Velocity view to the given response, using the given Velocity context which contains the complete
	 * template model to use.
	 * <p>
	 * The default implementation renders the template specified by the "url" bean property, retrieved via
	 * {@code getTemplate}. It delegates to the {@code mergeTemplate} method to merge the template instance with the
	 * given Velocity context.
	 * <p>
	 * Can be overridden to customize the behavior, for example to render multiple templates into a single view.
	 *
	 * @param context the Velocity context to use for rendering
	 * @param response servlet response (use this to get the OutputStream or Writer)
	 * @throws Exception if thrown by Velocity
	 * @see #setUrl
	 * @see #getTemplate()
	 * @see #mergeTemplate
	 */
	protected void doRender(Context context, HttpServletResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering Velocity template [" + getUrl() + "] in VelocityView '" + getBeanName() + "'");
		}
		mergeTemplate(getTemplate(), context, response);
	}

	/**
	 * Retrieve the Velocity template to be rendered by this view.
	 * <p>
	 * By default, the template specified by the "url" bean property will be retrieved: either returning a cached
	 * template instance or loading a fresh instance (according to the "cacheTemplate" bean property)
	 *
	 * @return the Velocity template to render
	 * @throws Exception if thrown by Velocity
	 * @see #setUrl
	 * @see #setCacheTemplate
	 * @see #getTemplate(String)
	 */
	protected Template getTemplate() throws Exception {
		// We already hold a reference to the template, but we might want to load it
		// if not caching. Velocity itself caches templates, so our ability to
		// cache templates in this class is a minor optimization only.
		if (isCacheTemplate() && this.template != null) {
			return this.template;
		} else {
			return getTemplate(getUrl());
		}
	}

	/**
	 * Retrieve the Velocity template specified by the given name, using the encoding specified by the "encoding" bean
	 * property.
	 * <p>
	 * Can be called by subclasses to retrieve a specific template, for example to render multiple templates into a
	 * single view.
	 *
	 * @param name the file name of the desired template
	 * @return the Velocity template
	 * @throws Exception if thrown by Velocity
	 * @see org.apache.velocity.app.VelocityEngine#getTemplate
	 */
	protected Template getTemplate(String name) throws Exception {
		return getVelocityEngine().getTemplate(name, Para.getConfig().defaultEncoding());
	}

	/**
	 * Merge the template with the context. Can be overridden to customize the behavior.
	 *
	 * @param template the template to merge
	 * @param context the Velocity context to use for rendering
	 * @param response servlet response (use this to get the OutputStream or Writer)
	 * @throws Exception if thrown by Velocity
	 * @see org.apache.velocity.Template#merge
	 */
	protected void mergeTemplate(
			Template template, Context context, HttpServletResponse response) throws Exception {

		try {
			response.setCharacterEncoding(Para.getConfig().defaultEncoding());
			template.merge(context, response.getWriter());
		} catch (MethodInvocationException ex) {
			Throwable cause = ex.getCause();
			throw new ServletException(
					"Method invocation failed during rendering of Velocity view with name '"
					+ getBeanName() + "': " + ex.getMessage() + "; reference [" + ex.getReferenceName()
					+ "], method '" + ex.getMethodName() + "'",
					cause == null ? ex : cause);
		}
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.api.ApiController.logger;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles various webhook events.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@RestController
@RequestMapping(value = "/webhooks", produces = "application/json")
public class WebhooksController {

	private final ScooldUtils utils;
	private final ParaClient pc;
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private static String lastConfigUpdate = null;

	@Inject
	public WebhooksController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@PostMapping("/config")
	public void updateConfig(HttpServletRequest req, HttpServletResponse res) throws JsonProcessingException {
		Map<String, Object> entity = readEntity(req);
		if (entity.containsKey("signature") && entity.containsKey("payload") &&
				entity.getOrDefault("event", "").equals("config.update")) {
			String payload = (String) entity.get("payload");
			String signature = (String) entity.get("signature");
			String id = (String) entity.get(Config._ID);
			boolean alreadyUpdated = id.equals(lastConfigUpdate);
			if (StringUtils.equals(signature, Utils.hmacSHA256(payload, CONF.paraSecretKey())) && !alreadyUpdated) {
				Map<String, Object> configMap = ParaObjectUtils.getJsonReader(Map.class).readValue(payload);
				configMap.entrySet().forEach((entry) -> {
					System.setProperty(entry.getKey(), entry.getValue().toString());
				});
				CONF.store();
				lastConfigUpdate = id;
			}
		}
	}

	private Map<String, Object> readEntity(HttpServletRequest req) {
		try {
			return ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
		} catch (Exception ex) {
			logger.error(null, ex);
		}
		return Collections.emptyMap();
	}

	public static void setLastConfigUpdate(String id) {
		lastConfigUpdate = id;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Webhook;
import com.erudika.para.core.annotations.Locked;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.para.core.validation.ValidationUtils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.AUTH_USER_ATTRIBUTE;
import static com.erudika.scoold.ScooldServer.REST_ENTITY_ATTRIBUTE;
import com.erudika.scoold.controllers.AdminController;
import com.erudika.scoold.controllers.CommentController;
import com.erudika.scoold.controllers.PeopleController;
import com.erudika.scoold.controllers.ProfileController;
import com.erudika.scoold.controllers.QuestionController;
import com.erudika.scoold.controllers.QuestionsController;
import com.erudika.scoold.controllers.ReportsController;
import com.erudika.scoold.controllers.RevisionsController;
import com.erudika.scoold.controllers.TagsController;
import com.erudika.scoold.controllers.VoteController;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import com.erudika.scoold.utils.BadRequestException;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.utils.Version;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Scoold REST API
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@RestController
@RequestMapping(value = "/api", produces = "application/json")
@SuppressWarnings("unchecked")
public class ApiController {

	public static final Logger logger = LoggerFactory.getLogger(ApiController.class);
	private static final String[] POST_TYPES = new String[] {Utils.type(Question.class), Utils.type(Reply.class)};

	private final ScooldUtils utils;
	private final ParaClient pc;
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	@Inject
	private QuestionsController questionsController;
	@Inject
	private QuestionController questionController;
	@Inject
	private VoteController voteController;
	@Inject
	private CommentController commentController;
	@Inject
	private PeopleController peopleController;
	@Inject
	private ProfileController profileController;
	@Inject
	private RevisionsController revisionsController;
	@Inject
	private TagsController tagsController;
	@Inject
	private ReportsController reportsController;
	@Inject
	private AdminController adminController;

	@Inject
	public ApiController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public Map<String, Object> get(HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isApiEnabled()) {
			res.setStatus(HttpStatus.FORBIDDEN.value());
			return null;
		}
		Map<String, Object> intro = healthCheck();
		boolean healthy = (boolean) intro.getOrDefault("healthy", false);
		if (!healthy) {
			res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return intro;
	}

	@GetMapping("/ping")
	public String ping(HttpServletRequest req, HttpServletResponse res) {
		if (!(boolean) healthCheck().getOrDefault("healthy", false)) {
			res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return "ponk";
		}
		return "pong";
	}

	@PostMapping("/posts")
	public Map<String, Object> createPost(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (!entity.containsKey(Config._TYPE)) {
			entity.put(Config._TYPE, POST_TYPES[0]);
		} else if (!StringUtils.equalsAnyIgnoreCase((CharSequence) entity.get(Config._TYPE), POST_TYPES)) {
			badReq("Invalid post type - could be one of " + Arrays.toString(POST_TYPES));
		}
		Post post = ParaObjectUtils.setAnnotatedFields(entity);

		if (!StringUtils.isBlank(post.getCreatorid())) {
			Profile authUser = pc.read(Profile.id(post.getCreatorid()));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		Model model = new ExtendedModelMap();
		List<String> spaces = readSpaces(post.getSpace());
		post.setSpace(spaces.iterator().hasNext() ? spaces.iterator().next() : null);

		if (post.isQuestion()) {
			questionsController.post(post.getLocation(), post.getLatlng(), post.getAddress(), post.getSpace(), post.getId(),
					req, res, model);
		} else if (post.isReply()) {
			questionController.reply(post.getParentid(), "", null, req, res, model);
		} else {
			badReq("Invalid post type - could be one of " + Arrays.toString(POST_TYPES));
		}

		checkForErrorsAndThrow(model);
		Map<String, Object> newpost = (Map<String, Object>) model.getAttribute("newpost");
		res.setStatus(HttpStatus.CREATED.value());
		return newpost;
	}

	@GetMapping("/posts")
	public List<Map<String, Object>> listQuestions(HttpServletRequest req) {
		Model model = new ExtendedModelMap();
		questionsController.getQuestions(req.getParameter("sortby"), req.getParameter("filter"), req, model);
		return ((List<Question>) model.getAttribute("questionslist")).stream().map(p -> {
			Map<String, Object> post = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(p, false));
			post.put("author", p.getAuthor());
			if (Boolean.parseBoolean(req.getParameter("includeReplies"))) {
				Pager itemcount = utils.getPager("pageReplies", req);
				post.put("children", questionController.getAllAnswers(utils.getSystemUser(), p, itemcount, req));
			}
			return post;
		}).collect(Collectors.toList());
	}

	@GetMapping("/posts/{id}")
	public Map<String, Object> getPost(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		questionController.get(id, "", req.getParameter("sortby"), req, res, model);
		Post showPost = (Post) model.getAttribute("showPost");
		List<Post> answers = (List<Post>) model.getAttribute("answerslist");
		List<Post> similar = (List<Post>) model.getAttribute("similarquestions");
		if (showPost == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		Map<String, Object> result = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(showPost, false));
		List<Map<String, Object>> answerz = answers.stream().map(p -> {
			Map<String, Object> post = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(p, false));
			post.put("author", p.getAuthor());
			return post;
		}).collect(Collectors.toList());
		result.put("comments", showPost.getComments());
		result.put("author", showPost.getAuthor());
		showPost.setItemcount(null);
		if (!showPost.isReply()) {
			result.put("children", answerz);
			result.put("similar", similar);
		}
		return result;
	}

	@PatchMapping("/posts/{id}")
	public Post updatePost(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		String editorid = (String) entity.get("lasteditby");
		if (!StringUtils.isBlank(editorid)) {
			Profile authUser = pc.read(Profile.id(editorid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		String space = (String) entity.get("space");
		String title = (String) entity.get("title");
		String body = (String) entity.get("body");
		String location = (String) entity.get("location");
		String latlng = (String) entity.get("latlng");
		List<String> spaces = readSpaces(space);
		space = spaces.iterator().hasNext() ? spaces.iterator().next() : null;
		Model model = new ExtendedModelMap();
		questionController.edit(id, title, body, String.join(",", (List<String>) entity.get("tags")),
				location, latlng, space, req, res, model);

		Post post = (Post) model.getAttribute("post");
		if (post == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
		} else if (!utils.canEdit(post, utils.getAuthUser(req))) {
			badReq("Update failed - user " + editorid + " is not allowed to update post.");
		}
		return post;
	}

	@DeleteMapping("/posts/{id}")
	public void deletePost(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		questionController.delete(id, req, model);
		res.setStatus(model.containsAttribute("deleted") ? 200 : HttpStatus.NOT_FOUND.value());
	}

	@PatchMapping("/posts/{id}/tags")
	public Post updatePostTags(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		String editorid = (String) entity.get("lasteditby");
		if (!StringUtils.isBlank(editorid)) {
			Profile authUser = pc.read(Profile.id(editorid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		questionController.get(id, "", req.getParameter("sortby"), req, res, model);
		Post post = (Post) model.getAttribute("showPost");
		if (post == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
		} else if (!utils.canEdit(post, utils.getAuthUser(req))) {
			badReq("Update failed - user " + editorid + " is not allowed to update post.");
		} else if (!entity.containsKey("add") && !entity.containsKey("remove")) {
			badReq("Request body is missing property values for 'add' or 'remove'.");
		} else {
			List<String> toAdd = (List<String>) entity.getOrDefault("add", Collections.emptyList());
			List<String> toRemove = (List<String>) entity.getOrDefault("remove", Collections.emptyList());
			Set<String> tags = new HashSet<>(post.getTags());
			if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
				tags.removeAll(toRemove);
				tags.addAll(toAdd);
				post.setTags(new ArrayList<>(tags));
				questionController.edit(id, post.getTitle(), post.getBody(), String.join(",", tags),
						post.getLocation(), post.getLatlng(), post.getSpace(), req, res, model);
			}
		}
		return post;
	}

	@PutMapping("/posts/{id}/approve")
	public void approvePost(@PathVariable String id, HttpServletRequest req) {
		questionController.modApprove(id, req);
	}

	@PutMapping("/posts/{id}/accept/{replyid}")
	public void acceptReply(@PathVariable String id, @PathVariable String replyid, HttpServletRequest req) {
		questionController.approve(id, replyid, req);
	}

	@PutMapping("/posts/{id}/close")
	public void closePost(@PathVariable String id, HttpServletRequest req) {
		questionController.close(id, req);
	}

	@PutMapping("/posts/{id}/pin")
	public void pinPost(@PathVariable String id, HttpServletRequest req) {
		badReq("Not supported");
	}

	@PutMapping("/posts/{id}/restore/{revisionid}")
	public void restoreRevision(@PathVariable String id, @PathVariable String revisionid, HttpServletRequest req) {
		questionController.restore(id, revisionid, req);
	}

	@PutMapping("/posts/{id}/like")
	public void favPost(@PathVariable String id, HttpServletRequest req) {
		badReq("Not supported");
	}

	@PutMapping("/posts/{id}/voteup")
	public void upvotePost(@PathVariable String id, @RequestParam(required = false) String userid,
			HttpServletRequest req, HttpServletResponse res) {
		if (!voteRequest(true, id, userid, req)) {
			badReq("Vote request failed.");
		}
	}

	@PutMapping("/posts/{id}/votedown")
	public void downvotePost(@PathVariable String id, @RequestParam(required = false) String userid,
			HttpServletRequest req) {
		if (!voteRequest(false, id, userid, req)) {
			badReq("Vote request failed.");
		}
	}

	@GetMapping("/posts/{id}/answers")
	public List<Reply> getPostReplies(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Post post = pc.read(id);
		if (post == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		Pager itemcount = utils.getPager("page", req);
		return questionController.getAllAnswers(utils.getAuthUser(req), post, itemcount, req);
	}

	@GetMapping("/posts/{id}/comments")
	public List<Comment> getPostComments(@PathVariable String id,
			@RequestParam(required = false, defaultValue = "5") String limit,
			@RequestParam(required = false, defaultValue = "1") String page,
			@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			@RequestParam(required = false, defaultValue = "false") String desc,
			HttpServletRequest req, HttpServletResponse res) {
		Post post = pc.read(id);
		if (post == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		post.getItemcount().setLimit(NumberUtils.toInt(limit));
		post.getItemcount().setPage(NumberUtils.toInt(page));
		post.getItemcount().setSortby(sortby);
		post.getItemcount().setDesc(Boolean.parseBoolean(desc));
		utils.reloadFirstPageOfComments(post);
		return post.getComments();
	}

	@GetMapping("/posts/{id}/revisions")
	public List<Map<String, Object>> getPostRevisions(@PathVariable String id,
			HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		revisionsController.get(id, req, res, model);
		Post post = (Post) model.getAttribute("showPost");
		if (post == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return ((List<Revision>) model.getAttribute("revisionslist")).stream().map(r -> {
			Map<String, Object> rev = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(r, false));
			rev.put("author", r.getAuthor());
			return rev;
		}).collect(Collectors.toList());
	}

	@PostMapping("/users")
	public Map<String, Object> createUser(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		Map<String, Object> userEntity = new HashMap<>();
		userEntity.put(Config._TYPE, Utils.type(User.class));
		userEntity.put(Config._NAME, entity.get(Config._NAME));
		userEntity.put(Config._EMAIL, entity.get(Config._EMAIL));
		userEntity.put(Config._IDENTIFIER, entity.get(Config._IDENTIFIER));
		userEntity.put(Config._GROUPS, entity.get(Config._GROUPS));
		userEntity.put("active", entity.getOrDefault("active", true));
		userEntity.put("picture", entity.get("picture"));

		User newUser = ParaObjectUtils.setAnnotatedFields(new User(), userEntity, null);
		newUser.setPassword((String) entity.getOrDefault(Config._PASSWORD, Utils.generateSecurityToken(10)));
		newUser.setIdentifier(StringUtils.isBlank(newUser.getIdentifier()) ? newUser.getEmail() : newUser.getIdentifier());
		String[] errors = ValidationUtils.validateObject(newUser);

		if (errors.length == 0) {
			// generic and password providers are identical but this was fixed in Para 1.37.1 (backwards compatibility)
			String provider = "generic".equals(newUser.getIdentityProvider()) ? "password" : newUser.getIdentityProvider();
			User createdUser = pc.signIn(provider, newUser.getIdentifier() + Para.getConfig().separator() +
					newUser.getName() + Para.getConfig().separator() + newUser.getPassword(), false);
			// user is probably active:false so activate them
			List<User> created = pc.findTerms(newUser.getType(), Collections.singletonMap(Config._EMAIL, newUser.getEmail()), true);
			if (createdUser == null && !created.isEmpty()) {
				createdUser = created.iterator().next();
				if (Utils.timestamp() - createdUser.getTimestamp() > TimeUnit.SECONDS.toMillis(20)) {
					createdUser = null; // user existed previously
				} else if (newUser.getActive() && !createdUser.getActive()) {
					createdUser.setActive(true);
					createdUser.setPicture(newUser.getPicture());
					pc.update(createdUser);
				}
			}
			if (createdUser == null) {
				badReq("Failed to create user. User may already exist.");
			} else {
				Profile profile = Profile.fromUser(createdUser);
				profile.setPicture(newUser.getPicture());
				profile.getSpaces().addAll(readSpaces(((List<String>) entity.getOrDefault("spaces",
						Collections.emptyList())).toArray(new String[0])));
				res.setStatus(HttpStatus.CREATED.value());
				pc.create(profile);

				Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(profile, false));
				payload.put("user", createdUser);
				utils.triggerHookEvent("user.signup", payload);
				logger.info("Created new user through API '{}' with id={}, groups={}, spaces={}.",
					createdUser.getName(), profile.getId(), profile.getGroups(), profile.getSpaces());
				Map<String, Object> result = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(profile, false));
				result.put("user", createdUser);
				return result;
			}
		}
		badReq("Failed to create user - " + String.join("; ", errors));
		return null;
	}

	@GetMapping("/users")
	public List<Map<String, Object>> listUsers(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			@RequestParam(required = false, defaultValue = "*") String q, HttpServletRequest req) {
		Model model = new ExtendedModelMap();
		peopleController.get(req.getParameter("tag"), sortby, q, req, model);
		List<Profile> profiles = (List<Profile>) model.getAttribute("userlist");
		List<Map<String, Object>> results = new LinkedList<>();
		Map<String, User> usersMap = new HashMap<>();
		if (profiles != null && !profiles.isEmpty()) {
			List<User> users = pc.readAll(profiles.stream().map(p -> p.getCreatorid()).collect(Collectors.toList()));
			for (User user : users) {
				usersMap.put(user.getId(), user);
			}
			for (Profile profile : profiles) {
				Map<String, Object> u = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(profile, false));
				u.put("user", usersMap.get(profile.getCreatorid()));
				results.add(u);
			}
		}
		return results;
	}

	@GetMapping("/users/{id}")
	public Map<String, Object> getUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		List<?> usrProfile = pc.readAll(Arrays.asList(StringUtils.substringBefore(id, Para.getConfig().separator()), Profile.id(id)));
		Iterator<?> it = usrProfile.iterator();
		User u = it.hasNext() ? (User) it.next() : null;
		Profile p = it.hasNext() ? (Profile) it.next() : null;
		if (p == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		Map<String, Object> result = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(p, false));
		result.put("user", u);
		return result;
	}

	@PatchMapping("/users/{id}")
	@SuppressWarnings("unchecked")
	public Profile updateUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		String name = (String) entity.get(Config._NAME);
		String email = (String) entity.get(Config._EMAIL);
		String password = (String) entity.get(Config._PASSWORD);
		String location = (String) entity.get("location");
		String latlng = (String) entity.get("latlng");
		String website = (String) entity.get("website");
		String aboutme = (String) entity.get("aboutme");
		String picture = (String) entity.get("picture");

		Model model = new ExtendedModelMap();
		profileController.edit(id, name, location, latlng, website, aboutme, picture, email, req, model);

		Profile profile = (Profile) model.getAttribute("user");
		if (profile == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		boolean update = false;
		if (entity.containsKey("spaces")) {
			profile.setSpaces(new HashSet<>(readSpaces(((List<String>) entity.getOrDefault("spaces",
						Collections.emptyList())).toArray(new String[0]))));
			update = true;
		}
		if (entity.containsKey("replyEmailsEnabled")) {
			profile.setReplyEmailsEnabled((Boolean) entity.get("replyEmailsEnabled"));
			update = true;
		}
		if (entity.containsKey("commentEmailsEnabled")) {
			profile.setCommentEmailsEnabled((Boolean) entity.get("commentEmailsEnabled"));
			update = true;
		}
		if (entity.containsKey("favtagsEmailsEnabled")) {
			profile.setFavtagsEmailsEnabled((Boolean) entity.get("favtagsEmailsEnabled"));
			update = true;
		}
		if (entity.containsKey("favtags") && entity.get("favtags") instanceof List) {
			profile.setFavtags((List<String>) entity.get("favtags"));
			update = true;
		}
		if (update) {
			pc.update(profile);
		}
		if (!StringUtils.isBlank(password)) {
			User u = profile.getUser();
			if (u == null || !StringUtils.equalsAny(u.getIdentityProvider(), "password", "generic")) {
				badReq("User's password cannot be modified.");
			}
			if (!utils.isPasswordStrongEnough(password)) {
				badReq("Password is not strong enough.");
			}
			Sysprop identifier = pc.read(u.getEmail());
			identifier.addProperty(Config._RESET_TOKEN, ""); // avoid removeProperty method because it won't be seen by server
			identifier.addProperty("iforgotTimestamp", 0);
			identifier.addProperty(Config._PASSWORD, Utils.bcrypt(password));
			pc.update(identifier);
		}
		return profile;
	}

	@DeleteMapping("/users/{id}")
	public void deleteUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile profile = pc.read(Profile.id(id));
		if (profile == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		profile.delete();
	}

	@GetMapping("/users/{id}/questions")
	public List<? extends Post> getUserQuestions(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile p = pc.read(Profile.id(id));
		if (p == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return profileController.getQuestions(utils.getAuthUser(req), p, true, utils.pagerFromParams(req));
	}

	@GetMapping("/users/{id}/replies")
	public List<? extends Post> getUserReplies(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile p = pc.read(Profile.id(id));
		if (p == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return profileController.getAnswers(utils.getAuthUser(req), p, true, utils.pagerFromParams(req));
	}

	@GetMapping("/users/{id}/favorites")
	public List<? extends Post> getUserFavorites(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		badReq("Not supported");
		return null;
	}

	@PutMapping("/users/{id}/moderator")
	public void makeUserMod(@PathVariable String id, @RequestParam(required = false, defaultValue = "") List<String> spaces,
			HttpServletRequest req, HttpServletResponse res) {
		profileController.makeMod(id, spaces, req, res);
	}

	@PutMapping("/users/{id}/ban")
	public void banUser(@PathVariable String id, @RequestParam(defaultValue = "0") String banuntil,
			HttpServletRequest req, HttpServletResponse res) {
		badReq("Not supported");
	}

	@PutMapping("/users/spaces")
	public void bulkEditSpaces(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		Set<String> selectedUsers = ((List<String>) entity.getOrDefault("users",
				Collections.emptyList())).stream().map(id -> Profile.id(id)).collect(Collectors.toSet());
		Set<String> selectedSpaces = ((List<String>) entity.getOrDefault("spaces",
				Collections.emptyList())).stream().distinct().collect(Collectors.toSet());
		Set<String> selectedBadges = ((List<String>) entity.getOrDefault("badges",
				Collections.emptyList())).stream().distinct().collect(Collectors.toSet());
		peopleController.bulkEdit(selectedUsers.toArray(String[]::new),
				readSpaces(selectedSpaces).toArray(String[]::new),
				selectedBadges.toArray(String[]::new), req);
	}

	@PostMapping("/tags")
	public Tag createTag(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		if (pc.read(new Tag((String) entity.get("tag")).getId()) != null) {
			badReq("Tag already exists.");
		}
		Tag newTag = ParaObjectUtils.setAnnotatedFields(new Tag((String) entity.get("tag")), entity, null);
		String[] errors = ValidationUtils.validateObject(newTag);
		if (errors.length == 0) {
			res.setStatus(HttpStatus.CREATED.value());
			return pc.create(newTag);
		}
		badReq("Failed to create tag - " + String.join("; ", errors));
		return null;
	}

	@GetMapping("/tags")
	public List<Tag> listTags(@RequestParam(required = false, defaultValue = "count") String sortby,
			HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		tagsController.get(sortby, req, model);
		return (List<Tag>) model.getAttribute("tagslist");
	}

	@GetMapping("/tags/{id}")
	public Tag getTag(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Tag tag = pc.read(new Tag(id).getId());
		if (tag == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return tag;
	}

	@PatchMapping("/tags/{id}")
	public Tag updateTag(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		Model model = new ExtendedModelMap();
		tagsController.rename(id, (String) entity.get("tag"), (String) entity.get("description"), req, res, model);
		if (!model.containsAttribute("tag")) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return (Tag) model.getAttribute("tag");
	}

	@DeleteMapping("/tags/{id}")
	public void deleteTag(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		tagsController.delete(id, req, res);
	}

	@GetMapping("/tags/{id}/questions")
	public List<Map<String, Object>> listTaggedQuestions(@PathVariable String id,
			HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		questionsController.getTagged(new Tag(id).getTag(), req, model);
		return ((List<Post>) model.getAttribute("questionslist")).stream().map(p -> {
			Map<String, Object> post = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(p, false));
			post.put("author", p.getAuthor());
			return post;
		}).collect(Collectors.toList());
	}

	@PostMapping("/comments")
	public Comment createComment(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		String comment = (String) entity.get("comment");
		String parentid = (String) entity.get(Config._PARENTID);
		String creatorid = (String) entity.get(Config._CREATORID);
		ParaObject parent = pc.read(parentid);
		if (parent == null) {
			badReq("Parent object not found. Provide a valid parentid.");
			return null;
		}
		if (!StringUtils.isBlank(creatorid)) {
			Profile authUser = pc.read(Profile.id(creatorid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		Model model = new ExtendedModelMap();
		commentController.createAjax(comment, parentid, req, model);
		Comment created = (Comment) model.getAttribute("showComment");
		if (created == null || StringUtils.isBlank(comment)) {
			badReq("Failed to create comment.");
			return null;
		}
		res.setStatus(HttpStatus.CREATED.value());
		return created;
	}

	@GetMapping("/comments/{id}")
	public Comment getComment(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Comment comment = pc.read(id);
		if (comment == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return comment;
	}

	@DeleteMapping("/comments/{id}")
	public void deleteComment(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		commentController.deleteAjax(id, req, res);
	}

	@PostMapping("/reports")
	public Report createReport(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		String creatorid = (String) entity.get(Config._CREATORID);
		if (!StringUtils.isBlank(creatorid)) {
			Profile authUser = pc.read(Profile.id(creatorid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		Model model = new ExtendedModelMap();
		reportsController.create(req, res, model);
		checkForErrorsAndThrow(model);
		Report newreport = (Report) model.getAttribute("newreport");
		res.setStatus(HttpStatus.CREATED.value());
		return newreport;
	}

	@GetMapping("/reports")
	public List<Report> listReports(@RequestParam(required = false, defaultValue = "count") String sortby,
			HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		reportsController.get(sortby, req, model);
		return (List<Report>) model.getAttribute("reportslist");
	}

	@GetMapping("/reports/{id}")
	public Report getReport(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Report report = pc.read(id);
		if (report == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return report;
	}

	@DeleteMapping("/reports/{id}")
	public void deleteReport(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		reportsController.delete(id, req, res);
	}

	@PutMapping("/reports/{id}/close")
	public void closeReport(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		String solution = (String) entity.getOrDefault("solution", "Closed via API.");
		reportsController.close(id, solution, req, res);
	}

	@PostMapping("/spaces")
	public Sysprop createSpace(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		String name = (String) entity.get(Config._NAME);
		if (StringUtils.isBlank(name)) {
			badReq("Property 'name' cannot be blank.");
			return null;
		}
		if (pc.read(utils.getSpaceId(name)) != null) {
			badReq("Space already exists.");
			return null;
		}
		Model model = new ExtendedModelMap();
		adminController.addSpace(name, "true".equals(req.getParameter("assigntoall")), req, res, model);
		checkForErrorsAndThrow(model);
		Sysprop s = (Sysprop) model.getAttribute("space");
		res.setStatus(HttpStatus.CREATED.value());
		return s;
	}

	@PatchMapping("/spaces/{id}")
	public void updateSpace(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		String newName = (String) entity.get(Config._NAME);
		if (StringUtils.isBlank(newName)) {
			badReq("Property 'name' cannot be blank.");
		}
		adminController.renameSpace(id, "true".equals(req.getParameter("assigntoall")),
				"true".equals(req.getParameter("needsapproval")), newName, req, res);
	}

	@GetMapping("/spaces")
	public List<Sysprop> listSpaces(HttpServletRequest req, HttpServletResponse res) {
		return pc.findQuery("scooldspace", "*", utils.pagerFromParams(req));
	}

	@GetMapping("/spaces/{id}")
	public Sysprop getSpace(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Sysprop space = pc.read(utils.getSpaceId(id));
		if (space == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return space;
	}

	@DeleteMapping("/spaces/{id}")
	public void deleteSpace(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		adminController.removeSpace(id, req, res);
	}

	@PostMapping("/webhooks")
	public Webhook createWebhook(HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isWebhooksEnabled()) {
			res.setStatus(HttpStatus.FORBIDDEN.value());
			return null;
		}
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
			return null;
		}
		Webhook w = ParaObjectUtils.setAnnotatedFields(new Webhook(), entity, null);
		if (StringUtils.isBlank(w.getTriggeredEvent()) && !Utils.isValidURL(w.getTargetUrl())) {
			badReq("Property 'targetUrl' must be a valid URL.");
			return null;
		}
		Webhook webhook = pc.create(w);
		if (webhook == null) {
			badReq("Failed to create webhook.");
			return null;
		}
		res.setStatus(HttpStatus.CREATED.value());
		return webhook;
	}

	@GetMapping("/webhooks")
	public List<Webhook> listWebhooks(HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isWebhooksEnabled()) {
			res.setStatus(HttpStatus.FORBIDDEN.value());
			return null;
		}
		return pc.findQuery(Utils.type(Webhook.class), "*", utils.pagerFromParams(req));
	}

	@GetMapping("/webhooks/{id}")
	public Webhook getWebhook(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isWebhooksEnabled()) {
			res.setStatus(HttpStatus.FORBIDDEN.value());
			return null;
		}
		Webhook webhook = pc.read(Utils.type(Webhook.class), id);
		if (webhook == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return webhook;
	}

	@PatchMapping("/webhooks/{id}")
	public Webhook updateWebhook(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isWebhooksEnabled()) {
			res.setStatus(HttpStatus.FORBIDDEN.value());
			return null;
		}
		Webhook webhook = pc.read(id);
		if (webhook == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		Map<String, Object> entity = readEntity(req);
		return pc.update(ParaObjectUtils.setAnnotatedFields(webhook, entity, Locked.class));
	}

	@DeleteMapping("/webhooks/{id}")
	public void deleteWebhook(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isWebhooksEnabled()) {
			res.setStatus(HttpStatus.FORBIDDEN.value());
		}
		Webhook webhook = pc.read(id);
		if (webhook == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		pc.delete(webhook);
	}

	@GetMapping("/events")
	public Set<String> listHookEvents(HttpServletRequest req, HttpServletResponse res) {
		return utils.getCustomHookEvents();
	}

	@GetMapping("/types")
	public Set<String> listCoreTypes(HttpServletRequest req, HttpServletResponse res) {
		return utils.getCoreScooldTypes();
	}

	@GetMapping("/search/{type}/{query}")
	public Map<String, Object> search(@PathVariable String type, @PathVariable String query, HttpServletRequest req) {
		if ("answer".equals(type)) {
			type = Utils.type(Reply.class);
		}
		Pager pager = utils.pagerFromParams(req);
		Map<String, Object> result = new HashMap<>();
		result.put("items", pc.findQuery(type, query, pager));
		result.put("page", pager.getPage());
		result.put("totalHits", pager.getCount());
		if (!StringUtils.isBlank(pager.getLastKey())) {
			result.put("lastKey", pager.getLastKey());
		}
		return result;
	}

	@GetMapping("/stats")
	public Map<String, Object> stats(HttpServletRequest req) {
		Map<String, Object> stats = new LinkedHashMap<>();
		long qcount = 0L;
		long acount = 0L;
		long scount = 0L;
		long ucount = 0L;
		long tcount = 0L;
		long rcount = 0L;
		long ccount = 0L;
		long recount = 0L;
		long uqcount = 0L;
		long uacount = 0L;
		String paraVer = null;
		try {
			Map<String, Number> typesCount = pc.typesCount();
			qcount = typesCount.getOrDefault(Utils.type(Question.class), 0).longValue();
			acount =  typesCount.getOrDefault(Utils.type(Reply.class), 0).longValue();
			scount =  typesCount.getOrDefault("scooldspace", 0).longValue();
			ucount =  typesCount.getOrDefault(Utils.type(Profile.class), 0).longValue();
			tcount =  typesCount.getOrDefault(Utils.type(Tag.class), 0).longValue();
			rcount =  typesCount.getOrDefault(Utils.type(Report.class), 0).longValue();
			ccount =  typesCount.getOrDefault(Utils.type(Comment.class), 0).longValue();
			recount = typesCount.getOrDefault(Utils.type(Revision.class), 0).longValue();
			uqcount = typesCount.getOrDefault(Utils.type(UnapprovedQuestion.class), 0).longValue();
			uacount = typesCount.getOrDefault(Utils.type(UnapprovedReply.class), 0).longValue();
			paraVer = pc.getServerVersion();
		} catch (Exception e) { }
		stats.put("questions", qcount);
		stats.put("replies", acount);
		stats.put("spaces", scount);
		stats.put("users", ucount);
		stats.put("tags", tcount);
		stats.put("reports", rcount);
		stats.put("comments", ccount);
		stats.put("revisions", recount);
		stats.put("unapproved_questions", uqcount);
		stats.put("unapproved_replies", uacount);
		stats.put("para_version", Optional.ofNullable(paraVer).orElse("unknown"));
		stats.put("scoold_version", Optional.ofNullable(Version.getVersion()).
				orElse(Optional.ofNullable(System.getenv("SCOOLD_VERSION")).orElse("unknown")));

		if ("true".equals(req.getParameter("includeLogs"))) {
			try {
				int maxLines = NumberUtils.toInt(req.getParameter("maxLogLines"), 10000);
				String logFile = System.getProperty("para.logs_dir", System.getProperty("user.dir"))
						+ "/" + System.getProperty("para.logs_name", "scoold") + ".log";
				Path path = Paths.get(logFile);
				try (Stream<String> lines = Files.lines(path)) {
					List<String> linez = lines.collect(Collectors.toList());
					stats.put("log", linez.subList(Math.max(0, linez.size() - maxLines), linez.size()).
							stream().collect(Collectors.joining("\n")));
				}
			} catch (Exception e) {
				logger.error("Failed to read log file. {}", e.getMessage());
			}
		}
		return stats;
	}

	@GetMapping(value = "/backup", produces = "application/zip")
	public ResponseEntity<StreamingResponseBody> backup(HttpServletRequest req, HttpServletResponse res) {
		return adminController.backup(req, res);
	}

	@PutMapping("/restore")
	public void restore(@RequestParam("file") MultipartFile file,
			@RequestParam(required = false, defaultValue = "false") Boolean isso,
			@RequestParam(required = false, defaultValue = "false") Boolean deleteall,
			HttpServletRequest req, HttpServletResponse res) {
		adminController.restore(file, isso, deleteall, req, res);
	}

	@GetMapping(path = "/config", produces = {"application/hocon", "application/json"})
	public String config(HttpServletRequest req, HttpServletResponse res) {
		String format = req.getParameter("format");
		if ("hocon".equalsIgnoreCase(format)) {
			res.setContentType("application/hocon");
			return CONF.render(false);
		} else {
			res.setContentType("application/json");
			return CONF.render(true);
		}
	}

	@PutMapping("/config")
	public String configSet(HttpServletRequest req, HttpServletResponse res) {
		com.typesafe.config.Config modifiedConf = com.typesafe.config.ConfigFactory.empty();
		if ("application/hocon".equals(req.getContentType())) {
			try {
				String config = IOUtils.toString(req.getInputStream(), "utf-8");
				for (Map.Entry<String, ConfigValue> entry : Config.parseStringWithoutIncludes(config).entrySet()) {
					modifiedConf = modifiedConf.withoutPath(entry.getKey()).withValue(entry.getKey(), entry.getValue());
					System.setProperty(entry.getKey(), entry.getValue().unwrapped().toString());
				}
			} catch (IOException ex) {
				badReq("Missing or invalid request body.");
			}
		} else {
			Map<String, Object> entity = readEntity(req);
			if (entity.isEmpty()) {
				badReq("Missing or invalid request body.");
			}
			for (Map.Entry<String, Object> entry : entity.entrySet()) {
				String key = CONF.getConfigRootPrefix() + "." + entry.getKey();
				modifiedConf = modifiedConf.withoutPath(key).withValue(key, ConfigValueFactory.fromAnyRef(entry.getValue()));
				System.setProperty(CONF.getConfigRootPrefix() + "." + entry.getKey(), entry.getValue().toString());
			}
		}
		CONF.overwriteConfig(modifiedConf).store();
		pc.setAppSettings(CONF.getParaAppSettings());
		triggerConfigUpdateEvent(CONF.getConfigMap());
		return config(req, res);
	}

	@GetMapping("/config/get/{key}")
	public Map<String, Object> configGet(@PathVariable String key, HttpServletRequest req, HttpServletResponse res) {
		Object value = null;
		try {
			value = CONF.getConfigValue(key, null);
		} catch (Exception e) {	}
		return Collections.singletonMap("value", value);
	}

	@PutMapping("/config/set/{key}")
	public void configSet(@PathVariable String key, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		com.typesafe.config.Config modifiedConf = CONF.getConfig();
		String kee = CONF.getConfigRootPrefix() + "." + key;
		Object value = entity.getOrDefault("value", null);
		if (value != null && !StringUtils.isBlank(value.toString())) {
			modifiedConf = modifiedConf.withValue(kee, ConfigValueFactory.fromAnyRef(value));
			System.setProperty(kee, value.toString());
		} else {
			modifiedConf = modifiedConf.withoutPath(kee);
			System.clearProperty(CONF.getConfigRootPrefix() + "." + key);
		}
		CONF.overwriteConfig(modifiedConf).store();
		if (CONF.getParaAppSettings().containsKey(key)) {
			pc.addAppSetting(key, value);
		}
		triggerConfigUpdateEvent(Collections.singletonMap(CONF.getConfigRootPrefix() + "." + key, value));
	}

	@GetMapping(path = "/config/options", produces = {"text/markdown", "application/hocon", "application/json"})
	public ResponseEntity<Object> configOptions(HttpServletRequest req, HttpServletResponse res) {
		String format = req.getParameter("format");
		String groupby = req.getParameter("groupby");
		if ("markdown".equalsIgnoreCase(format)) {
			res.setContentType("text/markdown");
		} else if ("hocon".equalsIgnoreCase(format)) {
			res.setContentType("application/hocon");
		} else if (StringUtils.isBlank(format) || "json".equalsIgnoreCase(format)) {
			res.setContentType("application/json");
		}
		return ResponseEntity.ok(CONF.renderConfigDocumentation(format,
				StringUtils.isBlank(groupby) || "category".equalsIgnoreCase(groupby)));
	}

	private boolean voteRequest(boolean isUpvote, String id, String userid, HttpServletRequest req) {
		if (!StringUtils.isBlank(userid)) {
			Profile authUser = pc.read(Profile.id(userid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		return isUpvote ? voteController.voteup(null, id, req) : voteController.votedown(null, id, req);
	}

	private List<String> readSpaces(Collection<String> spaces) {
		if (spaces == null || spaces.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> ids = spaces.stream().map(s -> utils.getSpaceId(s)).
				filter(s -> !s.isEmpty() && !utils.isDefaultSpace(s)).distinct().collect(Collectors.toList());
		List<Sysprop> existing = pc.readAll(ids);
		if (spaces.contains(Post.DEFAULT_SPACE) || spaces.contains("default")) {
			existing.add(utils.buildSpaceObject(Post.DEFAULT_SPACE));
		}
		return existing.stream().map(s -> s.getId() + Para.getConfig().separator() + s.getName()).collect(Collectors.toList());
	}

	private List<String> readSpaces(String... spaces) {
		return readSpaces(Arrays.asList(spaces));
	}

	private void triggerConfigUpdateEvent(Map<String, Object> payload) {
		int nodes = CONF.clusterNodes();
		if (nodes > 1) {
			Para.asyncExecute(() -> {
				Webhook trigger = new Webhook();
				trigger.setTimestamp(Utils.timestamp());
				trigger.setUpdate(true);
				trigger.setUpdateAll(true);
				trigger.setSecret("{{secretKey}}");
				trigger.setActive(true);
				trigger.setUrlEncoded(false);
				trigger.setTriggeredEvent("config.update");
				trigger.setCustomPayload(payload);
				trigger.setTargetUrl(CONF.serverUrl() + CONF.serverContextPath() + "/webhooks/config");
				// the goal is to saturate the load balancer and hopefully the payload reaches all nodes behind it
				trigger.setRepeatedDeliveryAttempts(nodes * 2);
				WebhooksController.setLastConfigUpdate(trigger.getTimestamp().toString());
				pc.create(trigger);
			});
		}
	}

	@ExceptionHandler({Exception.class})
	public Map<String, Object> handleException(Exception ex, WebRequest request, HttpServletResponse res) {
		Map<String, Object> error = new HashMap<>(2);
		int code = 500;
		if (ex instanceof BadRequestException) {
			code = 400;
		}
		res.setStatus(code);
		error.put("code", code);
		error.put("message", ex.getMessage());
		return error;
	}

	private Map<String, Object> healthCheck() {
		Map<String, Object> healthObj = new HashMap<>();
		healthObj.put("message", CONF.appName() + " API, see docs at " + CONF.serverUrl()
				+ CONF.serverContextPath() + "/apidocs");
		boolean healthy;
		try {
			healthy = pc != null && pc.getTimestamp() > 0;
		} catch (Exception e) {
			healthy = false;
		}
		healthObj.put("healthy", healthy);
		healthObj.put("pro", false);
		return healthObj;
	}

	private Map<String, Object> readEntity(HttpServletRequest req) {
		try {
			Map<String, Object> entity = ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
			req.setAttribute(REST_ENTITY_ATTRIBUTE, entity);
			return entity;
		} catch (IOException ex) {
			badReq("Expected 'application/json' body but got '" + req.getContentType() + "' in request body.");
		} catch (Exception ex) {
			logger.error(null, ex);
		}
		return Collections.emptyMap();
	}

	private void checkForErrorsAndThrow(Model model) {
		if (model != null && model.containsAttribute("error")) {
			Object err = model.getAttribute("error");
			if (err instanceof String) {
				badReq((String) err);
			} else if (err instanceof Map) {
				Map<String, String> error = (Map<String, String>) err;
				badReq(error.entrySet().stream().map(e -> "'" + e.getKey() + "' " +
						e.getValue()).collect(Collectors.joining("; ")));
			}
		}
	}

	private void badReq(String error) {
		if (!StringUtils.isBlank(error)) {
			throw new BadRequestException(error);
		}
	}

}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldServer;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class ApiDocsController {

	private final ScooldUtils utils;

	@Inject
	public ApiDocsController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping({"/apidocs", "/api.html"})
	public String get(HttpServletRequest req, Model model) {
		if (!utils.isApiEnabled()) {
			return "redirect:" + ScooldServer.HOMEPAGE;
		}
		if (req.getServletPath().endsWith(".html")) {
			return "redirect:" + ScooldServer.APIDOCSLINK;
		}
		model.addAttribute("path", "apidocs.vm");
		model.addAttribute("title", "API documentation");
		return "base";
	}

	@ResponseBody
	@GetMapping(path = "/api.json", produces = "text/javascript")
	public ResponseEntity<String> json(HttpServletRequest req, HttpServletResponse res) throws JsonProcessingException {
		if (!utils.isApiEnabled()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		Yaml yaml = new Yaml();
		String yml = utils.loadResource("templates/api.yaml");
		yml = StringUtils.replaceOnce(yml, "{{serverUrl}}", ScooldUtils.getConfig().serverUrl());
		yml = StringUtils.replaceOnce(yml, "{{contextPath}}", ScooldUtils.getConfig().serverContextPath());
		String result = ParaObjectUtils.getJsonWriter().writeValueAsString(yaml.load(yml));
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).eTag(Utils.md5(result)).body(result);
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import static com.erudika.scoold.ScooldServer.LANGUAGESLINK;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/languages")
public class LanguagesController {

	private final ScooldUtils utils;

	@Inject
	public LanguagesController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		model.addAttribute("path", "languages.vm");
		model.addAttribute("title", utils.getLang(req).get("translate.select"));
		Map<String, Integer> langProgressMap = utils.getLangutils().getTranslationProgressMap();
		model.addAttribute("langProgressMap", langProgressMap);
		model.addAttribute("allLocales", new TreeMap<>(utils.getLangutils().getAllLocales().entrySet().stream().
				filter(e -> langProgressMap.getOrDefault(e.getKey(), 0) > 70).
				collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()))));
		return "base";
	}

	@PostMapping("/{langkey}")
	public String post(@PathVariable String langkey, HttpServletRequest req, HttpServletResponse res) {
		Locale locale = utils.getCurrentLocale(langkey);
		if (locale != null) {
			int maxAge = 60 * 60 * 24 * 365;  //1 year
			HttpUtils.setRawCookie(ScooldUtils.getConfig().localeCookie(), locale.toString(), req, res, "Strict", maxAge);
		}
		return "redirect:" + LANGUAGESLINK;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.User;
import com.erudika.para.core.annotations.Email;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.SEARCHLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Sticky;
import com.erudika.scoold.utils.ScooldUtils;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class SearchController {

	private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public SearchController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping({"/search/{type}/{query}", "/search"})
	public String get(@PathVariable(required = false) String type, @PathVariable(required = false) String query,
			@RequestParam(required = false) String q, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + SEARCHLINK + "?q=" + Optional.ofNullable(query).orElse("*");
		}
		List<Profile> userlist = new ArrayList<Profile>();
		List<Post> questionslist = new ArrayList<Post>();
		List<Post> answerslist = new ArrayList<Post>();
		List<Post> feedbacklist = new ArrayList<Post>();
		List<Post> commentslist = new ArrayList<Post>();
		Pager itemcount = utils.getPager("page", req);
		String queryString = StringUtils.trimToEmpty(StringUtils.isBlank(q) ? query : q);
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString(queryString, req);
		boolean usersPublic = CONF.usersDiscoverabilityEnabled(utils.isAdmin(utils.getAuthUser(req)));

		if ("questions".equals(type)) {
			questionslist = utils.fullQuestionsSearch(qs, itemcount);
		} else if ("answers".equals(type)) {
			answerslist = pc.findQuery(Utils.type(Reply.class), qs, itemcount);
		} else if ("feedback".equals(type) && utils.isFeedbackEnabled()) {
			feedbacklist = pc.findQuery(Utils.type(Feedback.class), queryString, itemcount);
		} else if (("people".equals(type) || isEmailQuery(queryString)) && usersPublic) {
			userlist = searchUsers(queryString, req, itemcount);
		} else if ("comments".equals(type)) {
			commentslist = pc.findQuery(Utils.type(Comment.class), qs, itemcount);
		} else {
			questionslist = utils.fullQuestionsSearch(qs);
			answerslist = pc.findQuery(Utils.type(Reply.class), qs);
			if (utils.isFeedbackEnabled()) {
				feedbacklist = pc.findQuery(Utils.type(Feedback.class), queryString);
			}
			if (usersPublic) {
				userlist = searchUsers(queryString, req);
			}
			commentslist = pc.findQuery(Utils.type(Comment.class), qs, itemcount);
		}
		ArrayList<Post> list = new ArrayList<Post>();
		list.addAll(questionslist);
		list.addAll(answerslist);
		list.addAll(feedbacklist);
		utils.getProfiles(list);

		model.addAttribute("path", "search.vm");
		model.addAttribute("title", utils.getLang(req).get("search.title"));
		model.addAttribute("searchSelected", "navbtn-hover");
		model.addAttribute("showParam", type);
		model.addAttribute("searchQuery", queryString);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("userlist", userlist);
		model.addAttribute("questionslist", questionslist);
		model.addAttribute("answerslist", answerslist);
		model.addAttribute("feedbacklist", feedbacklist);
		model.addAttribute("commentslist", commentslist);

		triggerSearchEvent(type, qs, userlist.size() + questionslist.size() + answerslist.size() + commentslist.size(), req);
		return "base";
	}

	private String getUsersSearchQuery(String qs, HttpServletRequest req) {
		String spaceFilter = utils.sanitizeQueryString("", req).replaceAll("properties\\.space:", "properties.spaces:");
		return utils.getUsersSearchQuery(qs, spaceFilter);
	}

	private List<Profile> searchUsers(String queryString, HttpServletRequest req, Pager... pager) {
		if (isEmailQuery(queryString)) {
			List<String> uids = pc.findTerms(Utils.type(User.class),
					Map.of(Config._EMAIL, StringUtils.remove(queryString, "\"")), true).
					stream().map(u -> Profile.id(u.getId())).collect(Collectors.toList());
			return pc.findByIds(uids);
		} else {
			return pc.findQuery(Utils.type(Profile.class), getUsersSearchQuery(queryString, req), pager);
		}
	}

	private boolean isEmailQuery(String q) {
		return q.matches(Email.EMAIL_PATTERN) || StringUtils.remove(q, "\"").matches(Email.EMAIL_PATTERN);
	}

	@ResponseBody
	@GetMapping("/opensearch.xml")
	public ResponseEntity<String> openSearch(HttpServletRequest req) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ "<OpenSearchDescription xmlns=\"http://a9.com/-/spec/opensearch/1.1/\" "
				+ "  xmlns:moz=\"http://www.mozilla.org/2006/browser/search/\">\n"
				+ "  <ShortName>" + CONF.appName() + "</ShortName>\n"
				+ "  <Description>" + utils.getLang(req).get("search.description") + "</Description>\n"
				+ "  <InputEncoding>UTF-8</InputEncoding>\n"
				+ "  <Image width=\"16\" height=\"16\" type=\"image/x-icon\">" +
				CONF.serverUrl() + CONF.serverContextPath() + "/favicon.ico</Image>\n"
				+ "  <Url type=\"text/html\" method=\"get\" template=\"" + CONF.serverUrl() + CONF.serverContextPath()
				+ "/search?q={searchTerms}\"></Url>\n"
				+ "</OpenSearchDescription>";
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_XML).
				cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).
				eTag(Utils.md5(xml)).
				body(xml);
	}

	@ResponseBody
	@GetMapping("/manifest.webmanifest")
	public ResponseEntity<String> webmanifest(HttpServletRequest req) {
		String json = "{\n"
				+ "    \"theme_color\": \"#03a9f4\",\n"
				+ "    \"background_color\": \"#FFFFFF\",\n"
				+ "    \"display\": \"minimal-ui\",\n"
				+ "    \"scope\": \"/\",\n"
				+ "    \"start_url\": \"" + CONF.serverContextPath() + "/\",\n"
				+ "    \"name\": \"" + CONF.appName() + "\",\n"
				+ "    \"description\": \"" + CONF.metaDescription() + "\",\n"
				+ "    \"short_name\": \"" + CONF.appName() + "\",\n"
				+ "    \"icons\": [\n"
				+ "        {\n"
				+ "            \"src\": \"" + CONF.logoUrl() + "\",\n"
				+ "            \"sizes\": \"any\",\n"
				+ "            \"type\": \"image/svg-xml\"\n"
				+ "        },{\n"
				+ "            \"src\": \"" + CONF.imagesLink() + "/maskable512.png\",\n"
				+ "            \"sizes\": \"512x512\",\n"
				+ "            \"purpose\": \"maskable\",\n"
				+ "            \"type\": \"image/png\"\n"
				+ "        },{\n"
				+ "            \"src\": \"" + CONF.metaAppIconUrl() + "\",\n"
				+ "            \"sizes\": \"any\",\n"
				+ "            \"type\": \"image/png\"\n"
				+ "        }\n"
				+ "    ]\n"
				+ "}";
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_JSON).
				cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).
				eTag(Utils.md5(json)).
				body(json);
	}

	@GetMapping(path = "/feed.xml", produces = "application/rss+xml")
	public String feed(Model model, HttpServletRequest req, HttpServletResponse res) {
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString("*", req);
		boolean canList = utils.isDefaultSpacePublic() || utils.isAuthenticated(req);
		List<Post> questions = canList ? utils.fullQuestionsSearch(qs) : Collections.emptyList();
		List<Map<String, String>> entriez = new LinkedList<>();
		Map<String, String> lang = utils.getLang(req);
		String baseurl = CONF.serverUrl() + CONF.serverContextPath();
		baseurl = baseurl.endsWith("/") ? baseurl : baseurl + "/";

		model.addAttribute("title", Utils.formatMessage(lang.get("feed.title"), CONF.appName()));
		model.addAttribute("description", Utils.formatMessage(lang.get("feed.description"), CONF.appName()));
		model.addAttribute("baseurl", baseurl);
		model.addAttribute("updated", Utils.formatDate(Utils.timestamp(), "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH));

		for (Post post : questions) {
			String baselink = baseurl.concat("question/").concat(post.getId());
			Map<String, String> map = new HashMap<String, String>();
			map.put("url", baselink);
			map.put("title", post.getTitle());
			map.put("id", baselink.concat("/").concat(Utils.stripAndTrim(post.getTitle()).
					replaceAll("\\p{Z}+", "-").toLowerCase()));
			map.put("created", Utils.formatDate(post.getTimestamp(), "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH));
			map.put("updated", Utils.formatDate(post.getUpdated(), "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH));
			map.put("author", baseurl.concat("profile/").concat(post.getCreatorid()));
			map.put("body", StringUtils.removeEnd(Utils.markdownToHtml(post.getBody()), "\n"));
			entriez.add(map);
		}
		model.addAttribute("entries", entriez);
		res.setCharacterEncoding("UTF-8");
		res.setContentType("application/rss+xml");
		res.addHeader("Cache-Control", "max-age=3600");
		return "feed";
	}

	@ResponseBody
	@GetMapping("/sitemap.xml")
	public ResponseEntity<String> sitemap(HttpServletRequest req) {
		if (!CONF.sitemapEnabled()) {
			return ResponseEntity.notFound().build();
		}
		String sitemap = "";
		try {
			sitemap = getSitemap(req);
		} catch (Exception ex) {
			logger.error("Could not generate sitemap", ex);
		}
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_XML).
				cacheControl(CacheControl.maxAge(6, TimeUnit.HOURS)).
				eTag(Utils.md5(sitemap)).
				body(sitemap);
	}

	private String getSitemap(HttpServletRequest req) throws IOException {
		boolean canList = utils.isDefaultSpacePublic() || utils.isAuthenticated(req);
		if (canList) {
			List<Post> questions = new LinkedList<>();
			pc.readEverything(pager -> {
				pager.setLimit(100);
				List<Post> results = pc.findQuery("", Config._TYPE + ":(" + String.join(" OR ",
						Utils.type(Question.class), Utils.type(Sticky.class)) + ")", pager);
				questions.addAll(results);
				return results;
			});
			logger.debug("Found {} questions while generating sitemap.", questions.size());
			if (!questions.isEmpty()) {
				String baseurl = CONF.serverUrl() + CONF.serverContextPath();
				WebSitemapGenerator generator = new WebSitemapGenerator(baseurl);
				for (Post post : questions) {
					String baselink = baseurl.concat(post.getPostLink(false, false));
					generator.addUrl(new WebSitemapUrl.Options(baselink).lastMod(new Date(post.getTimestamp())).build());
				}
				return generator.writeAsStrings().get(0);
			}
		}
		logger.debug("Sitemap generation skipped - public={} auth={}", utils.isDefaultSpacePublic(), utils.isAuthenticated(req));
		return "<_/>";
	}

	private void triggerSearchEvent(String type, String query, int results, HttpServletRequest req) {
		if (req != null) {
			Profile authUser = utils.getAuthUser(req);
			Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(authUser, false));
			if (authUser != null) {
				payload.put("visitor", ParaObjectUtils.getAnnotatedFields(authUser, false));
			} else {
				payload.put("visitor", Collections.emptyMap());
			}
			Map<String, String> headers = new HashMap<>();
			headers.put(HttpHeaders.REFERER, req.getHeader(HttpHeaders.REFERER));
			headers.put(HttpHeaders.USER_AGENT, req.getHeader(HttpHeaders.USER_AGENT));
			headers.put("User-IP", req.getRemoteAddr());
			payload.put("headers", headers);
			payload.put("category", type);
			payload.put("query", query);
			payload.put("results", results);
			utils.triggerHookEvent("user.search", payload);
		}
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import static com.erudika.para.core.User.Groups.MODS;
import static com.erudika.para.core.User.Groups.USERS;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.PEOPLELINK;
import static com.erudika.scoold.ScooldServer.PROFILELINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Badge;
import com.erudika.scoold.core.Post;
import static com.erudika.scoold.core.Post.DEFAULT_SPACE;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Sticky;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.utils.avatars.*;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

	private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final ScooldUtils utils;
	private final ParaClient pc;
	private final GravatarAvatarGenerator gravatarAvatarGenerator;
	private final AvatarRepository avatarRepository;

	@Inject
	public ProfileController(ScooldUtils utils, GravatarAvatarGenerator gravatarAvatarGenerator, AvatarRepositoryProxy avatarRepository) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		this.gravatarAvatarGenerator = gravatarAvatarGenerator;
		this.avatarRepository = avatarRepository;
	}

	@GetMapping({"", "/{id}/**"})
	public String get(@PathVariable(required = false) String id, HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req) && StringUtils.isBlank(id)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PROFILELINK;
		}
		Profile authUser = utils.getAuthUser(req);
		Profile showUser;
		boolean isMyProfile;

		if (StringUtils.isBlank(id) || isMyid(authUser, Profile.id(id))) {
			//requested userid !exists or = my userid => show my profile
			showUser = authUser;
			isMyProfile = true;
		} else {
			showUser = pc.read(Profile.id(id));
			isMyProfile = isMyid(authUser, Profile.id(id));
		}

		if (showUser == null || !ParaObjectUtils.typesMatch(showUser)) {
			return "redirect:" + PROFILELINK;
		}

		boolean protekted = !utils.isDefaultSpacePublic() && !utils.isAuthenticated(req);
		boolean sameSpace = (utils.canAccessSpace(showUser, "default") && utils.canAccessSpace(authUser, "default")) ||
				(authUser != null && showUser.getSpaces().stream().anyMatch(s -> utils.canAccessSpace(authUser, s)));
		boolean profilesAreHidden = !isMyProfile && !CONF.usersDiscoverabilityEnabled(utils.isAdmin(authUser));
		if (protekted || !sameSpace || profilesAreHidden) {
			return "redirect:" + PEOPLELINK;
		}

		Pager itemcount1 = utils.getPager("page1", req);
		Pager itemcount2 = utils.getPager("page2", req);
		List<? extends Post> questionslist = getQuestions(authUser, showUser, isMyProfile, itemcount1);
		List<? extends Post> answerslist = getAnswers(authUser, showUser, isMyProfile, itemcount2);

		model.addAttribute("path", "profile.vm");
		model.addAttribute("title", showUser.getName());
		model.addAttribute("description", getUserDescription(showUser, itemcount1.getCount(), itemcount2.getCount()));
		model.addAttribute("ogimage", utils.getFullAvatarURL(showUser, AvatarFormat.Profile));
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		model.addAttribute("showUser", showUser);
		model.addAttribute("isMyProfile", isMyProfile);
		model.addAttribute("badgesCount", showUser.getBadgesMap().size() + showUser.getTags().size());
		model.addAttribute("tagsSet", new HashSet<>(showUser.getTags()));
		model.addAttribute("customBadgesMap", pc.findQuery(Utils.type(Badge.class), "*", new Pager(100)).stream().
				collect(Collectors.toMap(k -> ((Badge) k).getTag(), v -> v)));
		model.addAttribute("canEdit", isMyProfile || canEditProfile(authUser, id));
		model.addAttribute("canEditAvatar", CONF.avatarEditsEnabled());
		model.addAttribute("gravatarPicture", gravatarAvatarGenerator.getLink(showUser, AvatarFormat.Profile));
		model.addAttribute("isGravatarPicture", gravatarAvatarGenerator.isLink(showUser.getPicture()));
		model.addAttribute("includeEmojiPicker", true);
		model.addAttribute("itemcount1", itemcount1);
		model.addAttribute("itemcount2", itemcount2);
		model.addAttribute("questionslist", questionslist);
		model.addAttribute("answerslist", answerslist);
		model.addAttribute("nameEditsAllowed", CONF.nameEditsEnabled());
		return "base";
	}

	@PostMapping("/{id}/make-mod")
	public String makeMod(@PathVariable String id, @RequestParam(required = false, defaultValue = "") List<String> spaces,
			HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!isMyid(authUser, Profile.id(id))) {
			Profile showUser = pc.read(Profile.id(id));
			if (showUser != null) {
				if (utils.isAdmin(authUser) && !utils.isAdmin(showUser)) {
					if (CONF.modsAccessAllSpaces()) {
						showUser.setGroups(utils.isMod(showUser) ? USERS.toString() : MODS.toString());
					} else {
						for (String space : spaces) {
							if (showUser.isModInSpace(space)) {
								showUser.getModspaces().remove(space);
							} else {
								showUser.getModspaces().add(space);
							}
						}
					}
					showUser.update();
				}
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "base";
		} else {
			return "redirect:" + PROFILELINK + "/" + id;
		}
	}

	@PostMapping("/{id}")
	public String edit(@PathVariable(required = false) String id, @RequestParam(required = false) String name,
			@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String website, @RequestParam(required = false) String aboutme,
			@RequestParam(required = false) String picture, @RequestParam(required = false) String email,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		String queryString = "";
		if (showUser != null) {
			boolean updateProfile = false;
			if (!StringUtils.equals(showUser.getLocation(), location)) {
				showUser.setLatlng(latlng);
				showUser.setLocation(location);
				updateProfile = true;
			}
			if (!StringUtils.equals(showUser.getWebsite(), website) &&
					(StringUtils.isBlank(website) || Utils.isValidURL(website))) {
				showUser.setWebsite(website);
				updateProfile = true;
			}
			if (!StringUtils.equals(showUser.getAboutme(), aboutme)) {
				showUser.setAboutme(aboutme);
				updateProfile = true;
			}
			if (Utils.isValidEmail(email) && canChangeEmail(showUser.getUser(), email)) {
				if (utils.isAdmin(authUser) || CONF.allowUnverifiedEmails()) {
					changeEmail(showUser.getUser(), showUser, email);
				} else {
					if (!utils.isEmailDomainApproved(email)) {
						queryString = "?code=9&error=true";
					} else if (!isAvailableEmail(email)) {
						queryString = "?code=1&error=true";
					} else if (sendConfirmationEmail(showUser.getUser(), showUser, email, req)) {
						updateProfile = true;
						queryString = "?code=signin.verify.start&success=true";
					} else {
						queryString = "?code=signin.verify.fail&error=true";
					}
				}
			}

			updateProfile = updateUserPictureAndName(showUser, picture, name) || updateProfile;

			if (updateProfile) {
				showUser.update();
			}
			model.addAttribute("user", showUser);
		}
		return "redirect:" + PROFILELINK + (isMyid(authUser, id) ? "" : "/" + id) + queryString;
	}

	@SuppressWarnings("unchecked")
	@ResponseBody
	@PostMapping(value = "/{id}/cloudinary-upload-link", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> generateCloudinaryUploadLink(@PathVariable String id, HttpServletRequest req) {
		if (!ScooldUtils.isCloudinaryAvatarRepositoryEnabled()) {
			return ResponseEntity.status(404).build();
		}

		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		if (showUser == null) {
			return ResponseEntity.status(403).build();
		}

		String preset = "avatar";
		String publicId = "avatars/" + id;
		long timestamp = Utils.timestamp() / 1000;
		Cloudinary cloudinary = new Cloudinary(CONF.cloudinaryUrl());
		String signature = cloudinary.apiSignRequest(ObjectUtils.asMap(
			"public_id", publicId,
			"timestamp", String.valueOf(timestamp),
			"upload_preset", preset
		), cloudinary.config.apiSecret);

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("url", "https://api.cloudinary.com/v1_1/" + cloudinary.config.cloudName + "/image/upload");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("resource_type", "image");
		data.put("public_id", publicId);
		data.put("upload_preset", preset);
		data.put("filename", id);
		data.put("timestamp", timestamp);
		data.put("api_key", cloudinary.config.apiKey);
		data.put("signature", signature);
		response.put("data", data);

		return ResponseEntity.ok().body(response);
	}

	@PostMapping("/{id}/create-badge")
	public String createBadge(@PathVariable String id, @RequestParam String tag,
			@RequestParam(required = false, defaultValue = "") String description,
			@RequestParam(required = false, defaultValue = "#FFFFFF") String color,
			@RequestParam(required = false, defaultValue = "#555555") String background,
			@RequestParam(required = false, defaultValue = "") String icon,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		if (showUser != null && utils.isMod(authUser)) {
			Badge b = new Badge(tag);
			b.setIcon(icon);
			b.setStyle(Utils.formatMessage("background-color: {0}; color: {1};", background, color));
			b.setDescription(StringUtils.isBlank(description) ? tag : description);
			b.setCreatorid(authUser.getCreatorid());
			pc.create(b);
			showUser.addCustomBadge(b);
			showUser.update();
		}
		return "redirect:" + PROFILELINK + (isMyid(authUser, id) ? "" : "/" + id);
	}

	@PostMapping("/delete-badge/{id}")
	public String deleteBadge(@PathVariable String id, HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (id != null && utils.isMod(authUser)) {
			Badge b = pc.read(new Badge(id).getId());
			if (b != null) {
				pc.delete(b);
				pc.updateAllPartially((toUpdate, pager) -> {
					List<Profile> profiles = pc.findTagged(Utils.type(Profile.class), new String[]{id}, pager);
					for (Profile p : profiles) {
						p.removeCustomBadge(b.getTag());
						Map<String, Object> profile = new HashMap<>();
						profile.put(Config._ID, p.getId());
						profile.put(Config._TAGS, p.getTags().stream().
								filter(t -> !t.equals(id)).distinct().collect(Collectors.toList()));
						profile.put("customBadges", p.getCustomBadges());
						toUpdate.add(profile);
					}
					return profiles;
				});
			}
		}
		return "redirect:" + PROFILELINK + (isMyid(authUser, id) ? "" : "/" + id);
	}

	@PostMapping("/{id}/toggle-badge/{tag}")
	public ResponseEntity<?> toggleBadge(@PathVariable String id, @PathVariable String tag, HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		if (showUser != null && utils.isMod(authUser)) {
			Badge b = new Badge(tag);
			if (!showUser.removeCustomBadge(b.getTag())) {
				showUser.addCustomBadge(pc.read(b.getId()));
			}
			showUser.update();
		}
		return ResponseEntity.ok().build();
	}

	@GetMapping(path = "/confirm-email")
	public String confirmEmail(@RequestParam(name = "id", required = false) String id,
			@RequestParam(name = "token", required = false) String token,
			@RequestParam(name = "token2", required = false) String token2,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser == null || !CONF.passwordAuthEnabled()) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PROFILELINK;
		}
		if (id != null && (!StringUtils.isBlank(token) || !StringUtils.isBlank(token2))) {
			User u = (User) pc.read(id);
			Sysprop s = pc.read(u.getIdentifier());
			if (s != null && StringUtils.equals(token, (String) s.getProperty(Config._EMAIL_TOKEN))) {
				s.addProperty(Config._EMAIL_TOKEN, "");
				pc.update(s);
				if (StringUtils.isBlank((String) s.getProperty(Config._EMAIL_TOKEN + "2"))) {
					return changeEmail(u, authUser, authUser.getPendingEmail());
				}
				return "redirect:" + PROFILELINK + "?code=signin.verify.start&success=true";
			} else if (s != null && StringUtils.equals(token2, (String) s.getProperty(Config._EMAIL_TOKEN + "2"))) {
				s.addProperty(Config._EMAIL_TOKEN + "2", "");
				pc.update(s);
				if (StringUtils.isBlank((String) s.getProperty(Config._EMAIL_TOKEN))) {
					return changeEmail(u, authUser, authUser.getPendingEmail());
				}
				return "redirect:" + PROFILELINK + "?code=signin.verify.start&success=true";
			} else {
				return "redirect:" + SIGNINLINK;
			}
		}
		return "redirect:" + PROFILELINK;
	}

	@PostMapping(path = "/retry-change-email")
	public String retryChangeEmail(HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser == null) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PROFILELINK;
		}
		if (!StringUtils.isBlank(authUser.getPendingEmail())) {
			if (!isAvailableEmail(authUser.getPendingEmail())) {
				return "redirect:" + PROFILELINK + "?code=1&error=true";
			}
			if (!sendConfirmationEmail(authUser.getUser(), authUser, authUser.getPendingEmail(), req)) {
				return "redirect:" + PROFILELINK + "?code=signin.verify.fail&error=true";
			}
		}
		return "redirect:" + PROFILELINK + "?code=signin.verify.start&success=true";
	}

	@PostMapping(path = "/cancel-change-email")
	public String cancelChangeEmail(HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser == null) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PROFILELINK;
		}
		if (!StringUtils.isBlank(authUser.getPendingEmail())) {
			authUser.setPendingEmail("");
			User u = (User) pc.read(authUser.getCreatorid());
			Sysprop s = pc.read(u.getIdentifier());
			if (s != null) {
				s.removeProperty(Config._EMAIL_TOKEN);
				s.removeProperty(Config._EMAIL_TOKEN + "2");
				s.removeProperty("confirmationTimestamp");
				pc.updateAll(List.of(s, authUser));
			} else {
				authUser.update();
			}
		}
		return "redirect:" + PROFILELINK;
	}

	@PostMapping("/toggle-editor-role")
	public String toggleEditorRole(HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser != null && StringUtils.equalsAny(authUser.getGroups(),
				User.Groups.ADMINS.toString(), User.Groups.MODS.toString())) {
			authUser.setEditorRoleEnabled(!authUser.getEditorRoleEnabled());
			authUser.update();
		}
		return "redirect:" + HttpUtils.getBackToUrl(req, true);
	}

	private String changeEmail(User u, Profile showUser, String email) {
		boolean approvedDomain = utils.isEmailDomainApproved(email);
		if (approvedDomain && canChangeEmail(u, email)) {
			Sysprop s = pc.read(u.getEmail());
			if (s != null && pc.read(email) == null) {
				pc.delete(s);
				s.setId(email);
				pc.create(s);
				u.setEmail(email);
				showUser.setPendingEmail("");
				pc.updateAll(List.of(u, showUser));
				return "redirect:" + PROFILELINK + "?code=signin.verify.changed&success=true";
			} else {
				logger.info("Failed to change email for user {} - email {} has already been taken.", u.getId(), email);
				return "redirect:" + PROFILELINK + "?code=1&error=true";
			}
		}
		return "redirect:" + PROFILELINK + "?code=9&error=true";
	}

	private boolean sendConfirmationEmail(User user, Profile showUser, String email, HttpServletRequest req) {
		Sysprop ident = pc.read(user.getEmail());
		if (ident != null) {
			if (!ident.hasProperty("confirmationTimestamp") || Utils.timestamp() >
				((long) ident.getProperty("confirmationTimestamp") + TimeUnit.HOURS.toMillis(6))) {
				showUser.setPendingEmail(email);
				utils.sendVerificationEmail(ident, email, PROFILELINK + "/confirm-email", req);
				return true;
			} else {
				logger.warn("Failed to send email confirmation to '{}' - this can only be done once every 6h.", email);
			}
		}
		return false;
	}

	private boolean isAvailableEmail(String email) {
		boolean b = pc.read(email) == null && pc.findTerms(Utils.type(User.class), Map.of(Config._EMAIL, email), true).isEmpty();
		if (!b) {
			logger.info("Failed to send confirmation email to user - email {} has already been taken.", email);
		}
		return b;
	}

	private boolean canChangeEmail(User u, String email) {
		return "generic".equals(u.getIdentityProvider()) && !StringUtils.equals(u.getEmail(), email);
	}

	private Profile getProfileForEditing(String id, Profile authUser) {
		if (!canEditProfile(authUser, id)) {
			return null;
		}
		return isMyid(authUser, id) ? authUser : (Profile) pc.read(Profile.id(id));
	}

	private boolean updateUserPictureAndName(Profile showUser, String picture, String name) {
		boolean updateProfile = false;
		boolean updateUser = false;
		User u = showUser.getUser();

		if (CONF.avatarEditsEnabled() && !StringUtils.isBlank(picture)) {
			updateProfile = avatarRepository.store(showUser, picture);
		}

		if (CONF.nameEditsEnabled() && !StringUtils.isBlank(name)) {
			showUser.setName(StringUtils.abbreviate(name, 256));
			if (StringUtils.isBlank(showUser.getOriginalName())) {
				showUser.setOriginalName(name);
			}
			if (!u.getName().equals(name)) {
				u.setName(name);
				updateUser = true;
			}
			updateProfile = true;
		}

		if (updateUser) {
			pc.update(u);
		}
		return updateProfile;
	}

	private boolean isMyid(Profile authUser, String id) {
		return authUser != null && (StringUtils.isBlank(id) || authUser.getId().equals(Profile.id(id)));
	}

	private boolean canEditProfile(Profile authUser, String id) {
		return isMyid(authUser, id) || utils.isAdmin(authUser);
	}

	private Object getUserDescription(Profile showUser, Long questions, Long answers) {
		if (showUser == null) {
			return "";
		}
		return showUser.getVotes() + " points, "
				+ showUser.getBadgesMap().size() + " badges, "
				+ questions + " questions, "
				+ answers + " answers "
				+ Utils.abbreviate(showUser.getAboutme(), 150);
	}

	public List<? extends Post> getQuestions(Profile authUser, Profile showUser, boolean isMyProfile, Pager itemcount) {
		String spaceFilter = getSpaceFilter(authUser, isMyProfile);
		if (isMyProfile || utils.isMod(authUser)) {
			return pc.findQuery("", getTypeQuery(Utils.type(Question.class), Utils.type(Sticky.class),
					Utils.type(UnapprovedQuestion.class)) + " AND " + getAuthorQuery(showUser) + spaceFilter, itemcount);
		} else {
			return pc.findQuery("", getTypeQuery(Utils.type(Question.class), Utils.type(Sticky.class))
								+ " AND " + getAuthorQuery(showUser) + spaceFilter, itemcount);
		}
	}

	public List<? extends Post> getAnswers(Profile authUser, Profile showUser, boolean isMyProfile, Pager itemcount) {
		String spaceFilter = getSpaceFilter(authUser, isMyProfile);
		if (isMyProfile || utils.isMod(authUser)) {
			return pc.findQuery("", getTypeQuery(Utils.type(Reply.class), Utils.type(UnapprovedReply.class))
								+ " AND " + getAuthorQuery(showUser) + spaceFilter, itemcount);
		} else {
			return pc.findQuery("", getTypeQuery(Utils.type(Reply.class))
					+ " AND " + getAuthorQuery(showUser) + spaceFilter, itemcount);
		}
	}

	private String getTypeQuery(String... types) {
		return Config._TYPE + ":(" + String.join(" OR ", types) + ")";
	}

	private String getAuthorQuery(Profile showUser) {
		return Config._CREATORID + ":(\"" + showUser.getId() + "\")";
	}

	private String getSpaceFilter(Profile authUser, boolean isMyProfile) {
		String spaceFilter;
		if (utils.isMod(authUser) || isMyProfile) {
			spaceFilter = "";
		} else if (authUser != null && authUser.hasSpaces()) {
			spaceFilter = "(" + authUser.getSpaces().stream().map(s -> "properties.space:\"" + s + "\"").
					collect(Collectors.joining(" OR ")) + ")";
		} else {
			spaceFilter = "properties.space:\"" + DEFAULT_SPACE + "\"";
		}
		spaceFilter = StringUtils.isBlank(spaceFilter) ? "" : " AND " + spaceFilter;
		return spaceFilter;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.PEOPLELINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Badge;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/people")
public class PeopleController {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public PeopleController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping(path = {"", "/bulk-edit", "/tag/{tag}"})
	public String get(@PathVariable(required = false) String tag,
			@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			@RequestParam(required = false, defaultValue = "*") String q, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PEOPLELINK;
		}
		if (req.getServletPath().endsWith("/bulk-edit")) {
			return "redirect:" + PEOPLELINK + "?bulkedit=true";
		}
		Profile authUser = utils.getAuthUser(req);

		if (!CONF.usersDiscoverabilityEnabled(utils.isAdmin(authUser))) {
			return "redirect:" + HOMEPAGE;
		}

		getUsers(q, sortby, tag, authUser, req, model);
		model.addAttribute("path", "people.vm");
		model.addAttribute("title", utils.getLang(req).get("people.title"));
		model.addAttribute("peopleSelected", "navbtn-hover");

		if (req.getParameter("bulkedit") != null && utils.isAdmin(authUser)) {
			List<ParaObject> spaces = pc.findQuery("scooldspace", "*", new Pager(Config.DEFAULT_LIMIT));
			model.addAttribute("spaces", spaces);
			model.addAttribute("customBadgesMap", pc.findQuery(Utils.type(Badge.class), "*", new Pager(100)).stream().
				collect(Collectors.toMap(k -> ((Badge) k).getTag(), v -> v)));
		}
		return "base";
	}

	@PostMapping("/bulk-edit")
	public String bulkEdit(@RequestParam(required = false) String[] selectedUsers,
			@RequestParam(required = false) final String[] selectedSpaces,
			@RequestParam(required = false) final String[] selectedBadges,
			HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		boolean isAdmin = utils.isAdmin(authUser);
		String operation = req.getParameter("operation");
		String selection = req.getParameter("selection");
		if (isAdmin && ("all".equals(selection) || selectedUsers != null)) {
			List<String> spaces = (selectedSpaces == null || selectedSpaces.length == 0) ?
					Collections.emptyList() : Arrays.asList(selectedSpaces);
			List<String> badges = (selectedBadges == null || selectedBadges.length == 0) ?
					Collections.emptyList() : Arrays.asList(selectedBadges);

			pc.updateAllPartially((toUpdate, pager) -> {
				List<Profile> profiles;
				if (selection == null || "selected".equals(selection)) {
					profiles = pc.readAll(List.of(selectedUsers));
					bulkEditSpacesAndBadges(profiles, operation, spaces, badges, toUpdate);
					return Collections.emptyList();
				} else {
					profiles = pc.findQuery(Utils.type(Profile.class), "*", pager);
					bulkEditSpacesAndBadges(profiles, operation, spaces, badges, toUpdate);
					return profiles;
				}
			});
		}
		return "redirect:" + PEOPLELINK + (isAdmin ? "?" + req.getQueryString() : "");
	}

	@GetMapping("/avatar")
	public void avatar(HttpServletRequest req, HttpServletResponse res, Model model) {
		// prevents reflected XSS. see https://brutelogic.com.br/poc.svg
		// for some reason the CSP header is not sent on these responses by the ScooldInterceptor
		utils.setSecurityHeaders(utils.getCSPNonce(), req, res);
		HttpUtils.getDefaultAvatarImage(res);
	}

	@PostMapping("/apply-filter")
	public String applyFilter(@RequestParam(required = false) String sortby, @RequestParam(required = false) String tab,
			@RequestParam(required = false, defaultValue = "false") Boolean bulkedit,
			@RequestParam(required = false) String[] havingSelectedSpaces,
			@RequestParam(required = false) String[] notHavingSelectedSpaces,
			@RequestParam(required = false, defaultValue = "false") String compactViewEnabled,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isMod(authUser)) {
			if (req.getParameter("clear") != null) {
				HttpUtils.removeStateParam("users-filter", req, res);
				HttpUtils.removeStateParam("users-view-compact", req, res);
			} else {
				List<String> havingSpaces = (havingSelectedSpaces == null || havingSelectedSpaces.length == 0) ?
						Collections.emptyList() : Arrays.asList(havingSelectedSpaces);
				List<String> notHavingSpaces = (notHavingSelectedSpaces == null || notHavingSelectedSpaces.length == 0) ?
						Collections.emptyList() : Arrays.asList(notHavingSelectedSpaces);

				Pager p = utils.pagerFromParams(req);
				List<String> spacesList = new ArrayList<String>();
				for (String s : havingSpaces) {
					spacesList.add(s);
				}
				for (String s : notHavingSpaces) {
					spacesList.add("-" + s);
				}
				p.setSelect(spacesList);
				savePagerToCookie(req, res, p);
				HttpUtils.setRawCookie("users-view-compact", compactViewEnabled,
						req, res, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
			}
		}
		return "redirect:" + PEOPLELINK + (bulkedit ? "/bulk-edit" : "") + (StringUtils.isBlank(sortby) ? "" : "?sortby="
				+ Optional.ofNullable(StringUtils.trimToNull(sortby)).orElse(tab));
	}

	@SuppressWarnings("unchecked")
	public List<Profile> getUsers(String q, String sortby, String tag, Profile authUser, HttpServletRequest req, Model model) {
		Pager itemcount = getPagerFromCookie(req, utils.getPager("page", req), model);
		itemcount.setSortby(sortby);
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString(q, req);
		if (req.getParameter("bulkedit") != null && utils.isAdmin(authUser)) {
			qs = q;
		} else {
			qs = qs.replaceAll("properties\\.space:", "properties.spaces:");
		}

		if (!qs.endsWith("*") && q.equals("*")) {
			qs += " OR properties.groups:(admins OR mods)"; // admins are members of every space and always visible
		}

		if (!StringUtils.equalsAny(q.trim(), "", "*")) {
			String spaceFilter = utils.sanitizeQueryString("", req).replaceAll("properties\\.space:", "properties.spaces:");
			qs = utils.getUsersSearchQuery(q, spaceFilter);
		}

		Set<String> havingSpaces = Optional.ofNullable((Set<String>) model.getAttribute("havingSpaces")).orElse(Set.of());
		Set<String> notHavingSpaces = Optional.ofNullable((Set<String>) model.getAttribute("notHavingSpaces")).orElse(Set.of());
		String havingSpacesFilter = "";
		String notHavingSpacesFilter = "";
		if (!havingSpaces.isEmpty()) {
			havingSpacesFilter = "+\"" + String.join("\" +\"", havingSpaces) + "\" ";
		}
		if (!notHavingSpaces.isEmpty()) {
			notHavingSpacesFilter = "-\"" + String.join("\" -\"", notHavingSpaces) + "\"";
			if (havingSpaces.isEmpty()) {
				// at least one + keyword is needed otherwise no search results are returned
				havingSpacesFilter = "+\"" + utils.getDefaultSpace() + "\"";
			}
		}
		if (utils.isMod(authUser) && (!havingSpaces.isEmpty() || !notHavingSpaces.isEmpty())) {
			StringBuilder sb = new StringBuilder("*".equals(qs) ? "" : "(".concat(qs).concat(") AND "));
			sb.append("properties.spaces").append(":(").append(havingSpacesFilter).append(notHavingSpacesFilter).append(")");
			qs = sb.toString();
		}
		if (!StringUtils.isBlank(tag)) {
			StringBuilder sb = new StringBuilder("*".equals(qs) ? "" : "(".concat(qs).concat(") AND "));
			if (tag.startsWith("groups:")) {
				sb.append("properties.").append(tag);
			} else {
				sb.append(Config._TAGS).append(":(\"").append(tag).append("\")");
			}
			qs = sb.toString();
		}

		List<Profile> userlist = pc.findQuery(Utils.type(Profile.class), qs, itemcount);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("userlist", userlist);
		return userlist;
	}

	private Pager getPagerFromCookie(HttpServletRequest req, Pager defaultPager, Model model) {
		try {
			defaultPager.setName("default_pager");
			String cookie = HttpUtils.getCookieValue(req, "users-filter");
			if (StringUtils.isBlank(cookie)) {
				return defaultPager;
			}
			Pager pager = ParaObjectUtils.getJsonReader(Pager.class).readValue(Utils.base64dec(cookie));
			pager.setPage(defaultPager.getPage());
			pager.setLastKey(null);
			pager.setCount(0);
			if (!pager.getSelect().isEmpty()) {
				Set<String> havingSpaces = new HashSet<String>();
				Set<String> notHavingSpaces = new HashSet<String>();
				pager.getSelect().stream().forEach((s) -> {
					if (s.startsWith("-")) {
						notHavingSpaces.add(StringUtils.removeStart(s, "-"));
					} else {
						havingSpaces.add(s);
					}
				});
				pager.setSelect(null);
				model.addAttribute("havingSpaces", havingSpaces);
				model.addAttribute("notHavingSpaces", notHavingSpaces);
			}
			return pager;
		} catch (JsonProcessingException ex) {
			return Optional.ofNullable(defaultPager).orElse(new Pager(CONF.maxItemsPerPage()) {
				public String getName() {
					return "default_pager";
				}
			});
		}
	}

	private void savePagerToCookie(HttpServletRequest req, HttpServletResponse res, Pager p) {
		try {
			HttpUtils.setRawCookie("users-filter", Utils.base64enc(ParaObjectUtils.getJsonWriterNoIdent().
					writeValueAsBytes(p)), req, res, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
		} catch (JsonProcessingException ex) { }
	}

	private void bulkEditSpacesAndBadges(List<Profile> profiles, String operation,
			List<String> spaces, List<String> badges, List<Map<String, Object>> toUpdate) {
		boolean bulkEditBadges = !badges.isEmpty();
		final List<Badge> badgez;
		if (bulkEditBadges) {
			List<String> ids = badges.stream().map(b -> new Badge(b).getId()).
					filter(s -> !StringUtils.isBlank(s)).distinct().collect(Collectors.toList());
			badgez = pc.readAll(ids);
		} else {
			badgez = Collections.emptyList();
		}
		profiles.stream().filter(p -> !(utils.isMod(p) && CONF.modsAccessAllSpaces())).forEach(p -> {
			if ("add".equals(operation)) {
				if (bulkEditBadges) {
					badgez.forEach(badge -> p.addCustomBadge(badge));
				} else {
					p.getSpaces().addAll(spaces);
				}
			} else if ("remove".equals(operation)) {
				if (bulkEditBadges) {
					badgez.forEach(badge -> p.removeCustomBadge(badge.getTag()));
				} else {
					p.getSpaces().removeAll(spaces);
				}
			} else {
				if (bulkEditBadges) {
					p.setTags(new LinkedList<>());
					p.setCustomBadges(new LinkedList<>());
					badgez.forEach(badge -> p.addCustomBadge(badge));
				} else {
					p.setSpaces(new HashSet<String>(spaces));
				}
			}
			Map<String, Object> profile = new HashMap<>();
			profile.put(Config._ID, p.getId());
			if (bulkEditBadges) {
				profile.put("tags", p.getTags());
				profile.put("customBadges", p.getCustomBadges());
			} else {
				profile.put("spaces", p.getSpaces());
			}
			toUpdate.add(profile);
		});
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Address;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.QUESTIONSLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Profile.Badge;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.utils.avatars.AvatarFormat;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Produces;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/question")
public class QuestionController {

	public static final Logger logger = LoggerFactory.getLogger(QuestionController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public QuestionController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping({"/{id}", "/{id}/{title}", "/{id}/{title}/*"})
	public String get(@PathVariable String id, @PathVariable(required = false) String title,
			@RequestParam(required = false) String sortby, HttpServletRequest req, HttpServletResponse res, Model model) {

		Post showPost = pc.read(id);
		if (showPost == null || !ParaObjectUtils.typesMatch(showPost)) {
			return "redirect:" + QUESTIONSLINK;
		}
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canAccessSpace(authUser, showPost.getSpace())) {
			return "redirect:" + (utils.isDefaultSpacePublic() || utils.isAuthenticated(req) ?
					QUESTIONSLINK : SIGNINLINK + "?returnto=" + req.getRequestURI());
		} else if (!utils.isDefaultSpace(showPost.getSpace()) && pc.read(utils.getSpaceId(showPost.getSpace())) == null) {
			showPost.setSpace(Post.DEFAULT_SPACE);
			pc.update(showPost);
		}

		if (showPost instanceof UnapprovedQuestion && !(utils.isMine(showPost, authUser) || utils.isMod(authUser))) {
			return "redirect:" + QUESTIONSLINK;
		}

		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby("newest".equals(sortby) ? "timestamp" : "votes");
		List<Reply> answerslist = getAllAnswers(authUser, showPost, itemcount, req);
		showPost.setAnswercount(itemcount.getCount()); // autocorrect answer count
		LinkedList<Post> allPosts = new LinkedList<Post>();
		allPosts.add(showPost);
		allPosts.addAll(answerslist);
		utils.getProfiles(allPosts);
		utils.getComments(allPosts);
		utils.getLinkedComment(showPost, req);
		utils.getVotes(allPosts, authUser);
		utils.updateViewCount(showPost, req, res);

		model.addAttribute("path", "question.vm");
		model.addAttribute("title", showPost.getTitle());
		model.addAttribute("description", Utils.abbreviate(Utils.stripAndTrim(showPost.getBody(), " "), 195));
		model.addAttribute("keywords", showPost.getTagsString());
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("showPost", allPosts.removeFirst());
		model.addAttribute("answerslist", allPosts);
		model.addAttribute("similarquestions", utils.getSimilarPosts(showPost, new Pager(10)));
		model.addAttribute("maxCommentLength", CONF.maxCommentLength());
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		model.addAttribute("includeEmojiPicker", true);
		model.addAttribute("maxCommentLengthError", Utils.formatMessage(utils.getLang(req).get("maxlength"),
				CONF.maxCommentLength()));
		if (showPost.getAuthor() != null) {
			model.addAttribute("ogimage", utils.getFullAvatarURL(showPost.getAuthor(), AvatarFormat.Profile));
		}
		triggerQuestionViewEvent(showPost, req);
		return "base";
	}

	@PostMapping("/{id}/edit")
	public String edit(@PathVariable String id, @RequestParam(required = false) String title,
			@RequestParam(required = false) String body, @RequestParam(required = false) String tags,
			@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String space, HttpServletRequest req, HttpServletResponse res, Model model) {

		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			model.addAttribute("post", showPost);
			if (utils.isAjaxRequest(req)) {
				res.setStatus(400);
				return "blank";
			} else {
				return "redirect:" + req.getRequestURI(); // + "/edit-post-12345" ?
			}
		}
		boolean isQuestion = !showPost.isReply();
		Set<String> addedTags = new HashSet<>();
		Post beforeUpdate = null;
		try {
			beforeUpdate = (Post) BeanUtils.cloneBean(showPost);
		} catch (Exception ex) {
			logger.error(null, ex);
		}

		// body can be blank
		showPost.setBody(body);
		showPost.setLocation(location);
		showPost.setAuthor(authUser);
		if (isQuestion) {
			if (StringUtils.length(title) > 2) {
				showPost.setTitle(title);
			}
			addedTags = updateTags(showPost, tags);
			updateSpaces(showPost, authUser, space, req);
		}
		//note: update only happens if something has changed
		if (!showPost.equals(beforeUpdate)) {
			// create revision manually
			if (showPost.hasUpdatedContent(beforeUpdate)) {
				Revision.createRevisionFromPost(showPost, false);
			}
			updatePost(showPost, authUser, req);
			updateLocation(showPost, authUser, location, latlng);
			utils.addBadgeOnceAndUpdate(authUser, Badge.EDITOR, true);
			if (req.getParameter("notificationsDisabled") == null) {
				utils.sendUpdatedFavTagsNotifications(showPost, new ArrayList<>(addedTags), req);
			}
		}
		model.addAttribute("post", showPost);
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			res.setContentType("application/json");
			try {
				res.getWriter().println("{\"url\":\"" + getPostLink(showPost) + "\"}");
			} catch (IOException ex) { }
			return "blank";
		} else {
			return "redirect:" + showPost.getPostLinkForRedirect();
		}
	}

	@PostMapping({"/{id}", "/{id}/{title}", "/{id}/{title}/write"})
	public String reply(@PathVariable String id, @PathVariable(required = false) String title,
			@RequestParam(required = false) Boolean emailme, HttpServletRequest req,
			HttpServletResponse res, Model model) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (authUser == null || showPost == null) {
			if (utils.isAjaxRequest(req)) {
				res.setStatus(400);
				return "base";
			} else {
				return "redirect:" + QUESTIONSLINK + "/" + id;
			}
		}
		if (emailme != null) {
			followPost(showPost, authUser, emailme);
		} else if (!showPost.isClosed() && !showPost.isReply()) {
			//create new answer
			boolean needsApproval = CONF.answersNeedApproval() && utils.postsNeedApproval(req) && utils.userNeedsApproval(authUser);
			Reply answer = utils.populate(req, needsApproval ? new UnapprovedReply() : new Reply(), "body");
			Map<String, String> error = utils.validate(answer);
			answer = handleSpam(answer, authUser, error, req);
			if (!error.containsKey("body") && !StringUtils.isBlank(answer.getBody())) {
				answer.setTitle(showPost.getTitle());
				answer.setCreatorid(authUser.getId());
				answer.setParentid(showPost.getId());
				answer.setSpace(showPost.getSpace());
				addRepOnReplyOnce(showPost, authUser, false);
				answer.create();

				showPost.setAnswercount(showPost.getAnswercount() + 1);
				showPost.setLastactivity(System.currentTimeMillis());
				if (showPost.getAnswercount() >= CONF.maxRepliesPerPost()) {
					showPost.setCloserid("0");
				}
				// update without adding revisions
				pc.update(showPost);
				utils.addBadgeAndUpdate(authUser, Badge.EUREKA, answer.getCreatorid().equals(showPost.getCreatorid()));
				answer.setAuthor(authUser);
				model.addAttribute("showPost", showPost);
				model.addAttribute("answerslist", Collections.singletonList(answer));
				// send email to the question author
				utils.sendReplyNotifications(showPost, answer, needsApproval, req);
				model.addAttribute("newpost", getNewAnswerPayload(answer));
			} else {
				model.addAttribute("error", error);
				model.addAttribute("path", "question.vm");
				res.setStatus(400);
			}
			return "reply";
		} else {
			model.addAttribute("error", "Parent post doesn't exist or cannot have children.");
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "reply";
		} else {
			return "redirect:" + QUESTIONSLINK + "/" + id;
		}
	}

	@PostMapping("/{id}/approve")
	public String modApprove(@PathVariable String id, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (utils.isMod(authUser)) {
			if (showPost instanceof UnapprovedQuestion) {
				showPost.setType(Utils.type(Question.class));
				showPost.setApprovedby(authUser);
				pc.create(showPost);
				// this notification here is redundant
				//utils.sendNewPostNotifications(showPost, req);
				utils.triggerHookEvent("question.approve", showPost);
			} else if (showPost instanceof UnapprovedReply) {
				showPost.setType(Utils.type(Reply.class));
				showPost.setApprovedby(authUser);
				addRepOnReplyOnce(pc.read(showPost.getParentid()), (Profile) pc.read(showPost.getCreatorid()), true);
				pc.create(showPost);
				utils.triggerHookEvent("answer.approve", showPost);
			}
			utils.deleteReportsAfterModAction(showPost);
		}
		return "redirect:" + ((showPost == null) ? QUESTIONSLINK : showPost.getPostLinkForRedirect());
	}

	@PostMapping("/{id}/approve/{answerid}")
	public String approve(@PathVariable String id, @PathVariable String answerid, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (answerid != null && utils.canApproveReply(showPost, authUser)) {
			Reply answer = (Reply) pc.read(answerid);

			if (answer != null && answer.isReply()) {
				Profile author = pc.read(answer.getCreatorid());
				if (author != null && utils.isAuthenticated(req)) {
					boolean samePerson = author.equals(authUser);

					if (answerid.equals(showPost.getAnswerid())) {
						// Answer approved award - UNDO
						unApproveAnswer(authUser, author, showPost);
					} else {
						// fixes https://github.com/Erudika/scoold/issues/370
						if (!StringUtils.isBlank(showPost.getAnswerid())) {
							Reply prevAnswer = (Reply) pc.read(showPost.getAnswerid());
							Profile prevAuthor = pc.read(Optional.ofNullable(prevAnswer).
									orElse(new Reply()).getCreatorid());
							unApproveAnswer(authUser, prevAuthor, showPost);
						}
						// Answer approved award - GIVE
						showPost.setAnswerid(answerid);
						showPost.setApprovalTimestamp(Utils.timestamp());
						if (!samePerson) {
							author.addRep(CONF.answerApprovedRewardAuthor());
							authUser.addRep(CONF.answerApprovedRewardVoter());
							utils.addBadgeOnce(authUser, Badge.NOOB, true);
							pc.updateAll(Arrays.asList(author, authUser));
						}
						utils.triggerHookEvent("answer.accept",
								getAcceptedAnswerPayload(showPost, answer, authUser, author));
					}
					showPost.update();
				}
			}
		}
		return "redirect:" + showPost.getPostLinkForRedirect();
	}

	@PostMapping("/{id}/close")
	public String close(@PathVariable String id, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.isMod(authUser) && !showPost.isReply()) {
			if (showPost.isClosed()) {
				showPost.setCloserid("");
			} else {
				showPost.setCloserid(authUser.getId());
				utils.triggerHookEvent("question.close", showPost);
			}
			showPost.update();
		}
		return "redirect:" + showPost.getPostLinkForRedirect();
	}

	@PostMapping("/{id}/make-comment/{answerid}")
	public String makeComment(@PathVariable String id, @PathVariable String answerid, HttpServletRequest req) {
		Post question = pc.read(id);
		Post answer = pc.read(answerid);
		Profile authUser = utils.getAuthUser(req);
		if (question == null || answer == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.isMod(authUser) && answer.isReply()) {
			Profile author = pc.read(answer.getCreatorid());
			Comment c = new Comment();
			c.setParentid(answer.getParentid());
			c.setComment(answer.getBody());
			c.setCreatorid(answer.getCreatorid());
			c.setAuthorName(Optional.ofNullable(author).orElse(authUser).getName());
			c = pc.create(c);
			if (c != null) {
				question.addCommentId(c.getId());
				question.setAnswercount(question.getAnswercount() - 1);
				pc.update(question);
				answer.delete();
				return "redirect:" + question.getPostLinkForRedirect();
			}
		}
		return "redirect:" + QUESTIONSLINK + "/" + answer.getParentid();
	}

	@PostMapping("/{id}/restore/{revisionid}")
	public String restore(@PathVariable String id, @PathVariable String revisionid, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.canEdit(showPost, authUser)) {
			utils.addBadgeAndUpdate(authUser, Badge.BACKINTIME, true);
			showPost.restoreRevisionAndUpdate(revisionid);
		}
		return "redirect:" + showPost.getPostLinkForRedirect();
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable String id, HttpServletRequest req, Model model) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			model.addAttribute("post", showPost);
			return "redirect:" + req.getRequestURI();
		}
		if (!showPost.isReply()) {
			if ((utils.isMine(showPost, authUser) && utils.canDelete(showPost, authUser)) || utils.isMod(authUser)) {
				utils.deleteReportsAfterModAction(showPost);
				showPost.delete();
				model.addAttribute("deleted", true);
				return "redirect:" + QUESTIONSLINK + "?success=true&code=16";
			}
		} else if (showPost.isReply()) {
			Post parent = pc.read(showPost.getParentid());
			if ((utils.isMine(showPost, authUser) && utils.canDelete(showPost, authUser, parent.getAnswerid())) ||
					utils.isMod(authUser)) {
				parent.setAnswercount(parent.getAnswercount() - 1);
				parent.setAnswerid(showPost.getId().equals(parent.getAnswerid()) ? "" : parent.getAnswerid());
				parent.update();
				utils.deleteReportsAfterModAction(showPost);
				showPost.delete();
				model.addAttribute("deleted", true);
			}
		}
		return "redirect:" + showPost.getPostLinkForRedirect();
	}

	@PostMapping("/{id}/deprecate")
	public String deprecate(@PathVariable String id, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.canEdit(showPost, authUser)) {
			showPost.setDeprecated(!showPost.getDeprecated());
			showPost.update();
		}
		return "redirect:" + showPost.getPostLinkForRedirect();
	}

	@PostMapping("/{id}/merge-into")
	public String merge(@PathVariable String id, @RequestParam String id2, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Post targetPost = pc.read(id2);
		Profile authUser = utils.getAuthUser(req);
		if (!(utils.canEdit(showPost, authUser) && utils.canEdit(targetPost, authUser)) || showPost == null ||
				targetPost == null || showPost.isReply() || targetPost.isReply() || showPost.equals(targetPost)) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.canEdit(showPost, authUser) && utils.canEdit(targetPost, authUser)) {
			if (CONF.mergeQuestionBodies()) {
				targetPost.setBody(targetPost.getBody() + "\n\n" + showPost.getBody());
			}
			targetPost.setAnswercount(targetPost.getAnswercount() + showPost.getAnswercount());
			targetPost.setViewcount(targetPost.getViewcount() + showPost.getViewcount());
			if (showPost.hasFollowers()) {
				for (Map.Entry<String, String> entry : showPost.getFollowers().entrySet()) {
					User u = new User(entry.getKey());
					u.setEmail(entry.getValue());
					targetPost.addFollower(u);
				}
			}
			pc.readEverything(pager -> {
				List<Reply> answers = pc.getChildren(showPost, Utils.type(Reply.class), pager);
				for (Reply answer : answers) {
					answer.setParentid(targetPost.getId());
					answer.setTitle(targetPost.getTitle());
				}
				pc.createAll(answers); // overwrite
				return answers;
			});
			targetPost.update();
			showPost.delete();
			utils.deleteReportsAfterModAction(showPost);
		}
		return "redirect:" + targetPost.getPostLinkForRedirect();
	}

	@GetMapping("/find/{q}")
	@Produces("application/json")
	public ResponseEntity<List<ParaObject>> findAjax(@PathVariable String q, HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			res.setStatus(401);
			return ResponseEntity.status(401).body(Collections.emptyList());
		}
		String qs = utils.sanitizeQueryString(q + "*", req);
		Pager pager = new Pager(1, "votes", true, 10);
		return ResponseEntity.ok(pc.findQuery(Utils.type(Question.class), qs, pager));
	}

	private void changeSpaceForAllAnswers(Post showPost, String space) {
		if (showPost == null || showPost.isReply()) {
			return;
		}
		pc.readEverything(pager -> {
			List<Reply> answerslist = List.of();
			try {
				answerslist = pc.getChildren(showPost, Utils.type(Reply.class), pager);
				for (Reply reply : answerslist) {
					reply.setSpace(space);
				}
				if (!answerslist.isEmpty()) {
					pc.updateAll(answerslist);
					Thread.sleep(500);
				}
			} catch (InterruptedException ex) {
				logger.error(null, ex);
				Thread.currentThread().interrupt();
			}
			return answerslist;
		});
	}

	public List<Reply> getAllAnswers(Profile authUser, Post showPost, Pager itemcount, HttpServletRequest req) {
		if (showPost == null || showPost.isReply()) {
			return Collections.emptyList();
		}
		List<Reply> answers = new ArrayList<>();
		Pager p = new Pager(itemcount.getPage(), itemcount.getLimit());
		if (utils.postsNeedApproval(req) && (utils.isMine(showPost, authUser) || utils.isMod(authUser))) {
			answers.addAll(showPost.getUnapprovedAnswers(p));
		}
		answers.addAll(showPost.getAnswers(itemcount));
		itemcount.setCount(itemcount.getCount() + p.getCount());
		if (utils.postsNeedApproval(req) && authUser != null && !utils.isMod(authUser)) {
			List<UnapprovedReply> uanswerslist = pc.findQuery(Utils.type(UnapprovedReply.class),
					Config._PARENTID + ":\"" + showPost.getId() + "\" AND " +
							Config._CREATORID + ":\"" + authUser.getId() + "\"");
			itemcount.setCount(itemcount.getCount() + uanswerslist.size());
			answers.addAll(uanswerslist);
		}
		return answers;
	}

	private void updatePost(Post showPost, Profile authUser, HttpServletRequest req) {
		boolean isSpam = utils.isSpam(showPost, authUser, req);
		if (isSpam) {
			if (CONF.automaticSpamProtectionEnabled()) {
				return;
			} else {
				Report rep = new Report();
				rep.setName(showPost.getTitle());
				rep.setContent(Utils.abbreviate(Utils.markdownToHtml(showPost.getBody()), 2000));
				rep.setParentid(showPost.getId());
				rep.setCreatorid(authUser.getId());
				rep.setDescription("SPAM detected");
				rep.setSubType(Report.ReportType.SPAM);
				rep.setLink(showPost.getPostLink(false, false));
				rep.setAuthorName(authUser.getName());
				rep.addProperty(utils.getLang(req).get("spaces.title"), utils.getSpaceName(showPost.getSpace()));
				rep.create();
			}
		}
		showPost.setLasteditby(authUser.getId());
		showPost.setLastedited(System.currentTimeMillis());
		if (showPost.isQuestion()) {
			showPost.setLastactivity(System.currentTimeMillis());
			showPost.update();
		} else if (showPost.isReply()) {
			Post questionPost = pc.read(showPost.getParentid());
			if (questionPost != null) {
				showPost.setSpace(questionPost.getSpace());
				questionPost.setLastactivity(System.currentTimeMillis());
				pc.updateAll(Arrays.asList(showPost, questionPost));
			} else {
				// create revision here
				showPost.update();
			}
		}
	}

	private void updateLocation(Post showPost, Profile authUser, String location, String latlng) {
		if (!showPost.isReply() && !StringUtils.isBlank(latlng)) {
			Address addr = new Address(showPost.getId() + Para.getConfig().separator() + Utils.type(Address.class));
			addr.setAddress(location);
			addr.setCountry(location);
			addr.setLatlng(latlng);
			addr.setParentid(showPost.getId());
			addr.setCreatorid(authUser.getId());
			pc.create(addr);
		}
	}

	private Set<String> updateTags(Post showPost, String tags) {
		if (!StringUtils.isBlank(tags)) {
			List<String> newTags = Arrays.asList(StringUtils.split(tags, ","));
			HashSet<String> addedTags = new HashSet<>(newTags);
			addedTags.removeAll(new HashSet<>(Optional.ofNullable(showPost.getTags()).orElse(Collections.emptyList())));
			if (newTags.size() >= CONF.minTagsPerPost()) {
				showPost.updateTags(showPost.getTags(), newTags);
			}
			return addedTags;
		}
		return Collections.emptySet();
	}

	private void updateSpaces(Post showPost, Profile authUser, String space, HttpServletRequest req) {
		String validSpace = utils.getValidSpaceIdExcludingAll(authUser,
				Optional.ofNullable(space).orElse(showPost.getSpace()), req);
		if (utils.canAccessSpace(authUser, validSpace) && validSpace != null
				&& !utils.getSpaceId(validSpace).equals(utils.getSpaceId(showPost.getSpace()))) {
			showPost.setSpace(validSpace);
			changeSpaceForAllAnswers(showPost, validSpace);
		}
	}

	private Map<String, Object> getNewAnswerPayload(Reply answer) {
		Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(answer, false));
		payload.put("author", answer == null ? null : answer.getAuthor());
		utils.triggerHookEvent("answer.create", payload);
		return payload;
	}

	private Object getAcceptedAnswerPayload(Post showPost, Reply answer, Profile authUser, Profile author) {
		Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(showPost, false));
		Map<String, Object> answerPayload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(answer, false));
		answerPayload.put("author", author);
		payload.put("children", answerPayload);
		payload.put("authUser", authUser);
		return payload;
	}

	private void followPost(Post showPost, Profile authUser, Boolean emailme) {
		if (emailme) {
			showPost.addFollower(authUser.getUser());
		} else {
			showPost.removeFollower(authUser.getUser());
		}
		pc.update(showPost); // update without adding revisions
	}

	private String getPostLink(Post showPost) {
		return showPost.getPostLink(false, false) + (showPost.isQuestion() ? "" :  "#post-" + showPost.getId());
	}

	private void unApproveAnswer(Profile authUser, Profile author, Post showPost) {
		if (showPost != null) {
			showPost.setAnswerid("");
			showPost.setApprovalTimestamp(0L);
		}
		if (author != null && !author.equals(authUser)) {
			author.removeRep(CONF.answerApprovedRewardAuthor());
			authUser.removeRep(CONF.answerApprovedRewardVoter());
			pc.updateAll(Arrays.asList(author, authUser));
		}
	}

	private void triggerQuestionViewEvent(Post question, HttpServletRequest req) {
		if (req != null) {
			Profile authUser = utils.getAuthUser(req);
			Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(authUser, false));
			if (authUser != null) {
				payload.put("visitor", ParaObjectUtils.getAnnotatedFields(authUser, false));
			} else {
				payload.put("visitor", Collections.emptyMap());
			}
			Map<String, String> headers = new HashMap<>();
			headers.put(HttpHeaders.REFERER, req.getHeader(HttpHeaders.REFERER));
			headers.put(HttpHeaders.USER_AGENT, req.getHeader(HttpHeaders.USER_AGENT));
			headers.put("User-IP", req.getRemoteAddr());
			payload.put("headers", headers);
			payload.put("question", question);
			utils.triggerHookEvent("question.view", payload);
		}
	}

	private void addRepOnReplyOnce(Post parentPost, Profile author, boolean isModAction) {
		if ((!CONF.postsNeedApproval() || isModAction) && CONF.answerCreatedRewardAuthor() > 0 &&
				!parentPost.getCreatorid().equals(author.getId()) && pc.getCount(Utils.type(Reply.class),
						Map.of(Config._PARENTID, parentPost.getId(), Config._CREATORID, author.getId())) == 0) {
			author.addRep(CONF.answerCreatedRewardAuthor());
			pc.update(author);
		}
	}

	private Reply handleSpam(Reply a, Profile authUser, Map<String, String> error, HttpServletRequest req) {
		boolean isSpam = utils.isSpam(a, authUser, req);
		if (isSpam && CONF.automaticSpamProtectionEnabled()) {
			error.put("body", "spam");
		} else if (isSpam && !CONF.automaticSpamProtectionEnabled()) {
			UnapprovedReply spama = new UnapprovedReply();
			spama.setTitle(a.getTitle());
			spama.setBody(a.getBody());
			spama.setTags(a.getTags());
			spama.setCreatorid(a.getCreatorid());
			spama.setParentid(a.getParentid());
			spama.setAuthor(authUser);
			spama.setSpace(a.getSpace());
			spama.setSpam(true);
			return spama;
		}
		return a;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.utils.Pager;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static com.erudika.scoold.ScooldServer.QUESTIONSLINK;
import com.erudika.scoold.core.Profile;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/revisions")
public class RevisionsController {

	private final ScooldUtils utils;

	@Inject
	public RevisionsController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping("/{postid}")
	public String get(@PathVariable String postid, HttpServletRequest req, HttpServletResponse res, Model model) {
		Post showPost = utils.getParaClient().read(postid);
		if (showPost == null) {
			return "redirect:" + QUESTIONSLINK;
		}
		res.setHeader("X-Robots-Tag", "noindex, nofollow"); // https://github.com/Erudika/scoold/issues/254
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canAccessSpace(authUser, showPost.getSpace())) {
			return "redirect:" + QUESTIONSLINK;
		}
		Pager itemcount = utils.getPager("page", req);
		List<Revision> revisionslist = showPost.getRevisions(itemcount);
		// we need the first revision on the next page for diffing
		List<Revision> nextPage = showPost.getRevisions(new Pager(itemcount.getPage() + 1, itemcount.getLimit()));
		utils.getProfiles(revisionslist);
		model.addAttribute("path", "revisions.vm");
		model.addAttribute("title", utils.getLang(req).get("revisions.title"));
		model.addAttribute("showPost", showPost);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("revisionslist", revisionslist);
		model.addAttribute("lastOnPage", nextPage.isEmpty() ? null : nextPage.get(0));
		return "base";
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.App;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Webhook;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.ADMINLINK;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.utils.ScooldUtils;
import static com.erudika.scoold.utils.ScooldUtils.MAX_SPACES;
import com.erudika.scoold.utils.Version;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nimbusds.jwt.SignedJWT;
import com.typesafe.config.ConfigValueFactory;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final String soDateFormat1 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private final String soDateFormat2 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public AdminController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req) && !utils.isAdmin(utils.getAuthUser(req))) {
			return "redirect:" + HOMEPAGE;
		} else if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + ADMINLINK;
		}
		Map<String, Object> configMetadata = new LinkedHashMap<String, Object>();
		if (CONF.configEditingEnabled()) {
			try {
				configMetadata = ParaObjectUtils.getJsonReader(Map.class).
						readValue(CONF.renderConfigDocumentation("json", true));
			} catch (IOException ex) { }
		}

		Pager itemcount = utils.getPager("page", req);
		Pager itemcount1 = utils.getPager("page1", req);
		itemcount.setLimit(40);
		model.addAttribute("path", "admin.vm");
		model.addAttribute("title", utils.getLang(req).get("administration.title"));
		model.addAttribute("configMap", CONF);
		model.addAttribute("configMetadata", configMetadata);
		model.addAttribute("version", pc.getServerVersion());
		model.addAttribute("endpoint", CONF.redirectUri());
		model.addAttribute("paraapp", CONF.paraAccessKey());
		model.addAttribute("spaces", getSpaces(itemcount));
		model.addAttribute("webhooks", pc.findQuery(Utils.type(Webhook.class), "*", itemcount1));
		model.addAttribute("scooldimports", pc.findQuery("scooldimport", "*", new Pager(7)));
		model.addAttribute("coreScooldTypes", utils.getCoreScooldTypes());
		model.addAttribute("customHookEvents", utils.getCustomHookEvents());
		model.addAttribute("apiKeys", utils.getApiKeys());
		model.addAttribute("apiKeysExpirations", utils.getApiKeysExpirations());
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("itemcount1", itemcount1);
		model.addAttribute("isDefaultSpacePublic", utils.isDefaultSpacePublic());
		model.addAttribute("scooldVersion", Version.getVersion());
		model.addAttribute("scooldRevision", Version.getRevision());
		String importedCount = req.getParameter("imported");
		if (importedCount != null) {
			if (req.getParameter("success") != null) {
				model.addAttribute("infoStripMsg", "Started a new data import task. ");
			} else {
				model.addAttribute("infoStripMsg", "Data import task failed! The archive was partially imported.");
			}
		}
		Sysprop theme = utils.getCustomTheme();
		String themeCSS = (String) theme.getProperty("theme");
		model.addAttribute("selectedTheme", theme.getName());
		model.addAttribute("customTheme", StringUtils.isBlank(themeCSS) ? utils.getDefaultTheme() : themeCSS);
		return "base";
	}

	@PostMapping("/add-space")
	public String addSpace(@RequestParam String space,
			@RequestParam(required = false, defaultValue = "false") Boolean assigntoall,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(space) && utils.isAdmin(authUser)) {
			Sysprop spaceObj = utils.buildSpaceObject(space);
			if (utils.isDefaultSpace(spaceObj.getId()) || pc.getCount("scooldspace") >= MAX_SPACES ||
					pc.read(spaceObj.getId()) != null) {
				model.addAttribute("error", Map.of("name", "Space exists or maximum number of spaces reached."));
			} else {
				if (assigntoall) {
					spaceObj.setTags(List.of("assign-to-all"));
					utils.assingSpaceToAllUsers(spaceObj);
				}
				if (pc.create(spaceObj) != null) {
					authUser.getSpaces().add(spaceObj.getId() + Para.getConfig().separator() + spaceObj.getName());
					authUser.update();
					model.addAttribute("space", spaceObj);
					utils.addSpaceToCachedList(spaceObj);
				} else {
					model.addAttribute("error", Collections.singletonMap("name", utils.getLang(req).get("posts.error1")));
				}
			}
		} else {
			model.addAttribute("error", Collections.singletonMap("name", utils.getLang(req).get("requiredfield")));
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(model.containsAttribute("error") ? 400 : 200);
			return "space";
		} else {
			return "redirect:" + ADMINLINK;
		}
	}

	@PostMapping("/remove-space")
	public String removeSpace(@RequestParam String space, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(space) && !utils.isDefaultSpace(space) && utils.isAdmin(authUser)) {
			Sysprop s = utils.buildSpaceObject(space);
			pc.delete(s);
			authUser.getSpaces().remove(space);
			authUser.update();
			utils.removeSpaceFromCachedList(s);
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "space";
		} else {
			return "redirect:" + ADMINLINK;
		}
	}

	@PostMapping("/rename-space")
	public String renameSpace(@RequestParam String space,
			@RequestParam(required = false, defaultValue = "false") Boolean assigntoall,
			@RequestParam(required = false, defaultValue = "false") Boolean needsapproval,
			@RequestParam String newspace, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		Sysprop s = pc.read(utils.getSpaceId(space));
		if (s != null && !utils.isDefaultSpace(space) && utils.isAdmin(authUser)) {
			String origSpace = s.getId() + Para.getConfig().separator() + s.getName();
			String newSpace = s.getId() + Para.getConfig().separator() + newspace;
			if (!origSpace.equals(newSpace)) {
				s.setName(newspace);
				pc.updateAllPartially((toUpdate, pager) -> {
					String query = "properties.spaces:(\"" + origSpace + "\")";
					List<Profile> profiles = pc.findQuery(Utils.type(Profile.class), query, pager);
					profiles.stream().forEach(p -> {
						p.getSpaces().remove(origSpace);
						p.getSpaces().add(newSpace);
						Map<String, Object> profile = new HashMap<>();
						profile.put(Config._ID, p.getId());
						profile.put("spaces", p.getSpaces());
						toUpdate.add(profile);
					});
					return profiles;
				});
				pc.updateAllPartially((toUpdate, pager) -> {
					String query = "properties.space:(\"" + origSpace + "\")";
					List<Post> posts = pc.findQuery("", query, pager);
					posts.stream().forEach(p -> {
						Map<String, Object> post = new HashMap<>();
						post.put(Config._ID, p.getId());
						post.put("space", newSpace);
						toUpdate.add(post);
					});
					return posts;
				});
			}
			if (utils.isAutoAssignedSpace(s) ^ assigntoall) {
				s.setTags(assigntoall ? List.of("assign-to-all") : List.of());
				utils.assingSpaceToAllUsers(assigntoall ? s : null);
			}

			s.addProperty("posts_need_approval", needsapproval && CONF.postsNeedApproval());
			pc.update(s);
			utils.getAllSpacesAdmin().parallelStream().
					filter(ss -> ss.getId().equals(s.getId())).
					forEach(e -> {
						e.setName(s.getName());
						e.setTags(s.getTags());
						e.setProperties(s.getProperties());
					});
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "space";
		} else {
			return "redirect:" + ADMINLINK;
		}
	}

	@PostMapping("/create-webhook")
	public String createWebhook(@RequestParam String targetUrl, @RequestParam(required = false) String type,
			@RequestParam Boolean json, @RequestParam Set<String> events, @RequestParam(required = false) String filter,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (Utils.isValidURL(targetUrl) && utils.isAdmin(authUser) && utils.isWebhooksEnabled()) {
			Webhook webhook = new Webhook(targetUrl);
			webhook.setCreate(events.contains("create"));
			webhook.setUpdate(events.contains("update"));
			webhook.setDelete(events.contains("delete"));
			webhook.setCreateAll(events.contains("createAll"));
			webhook.setUpdateAll(events.contains("updateAll"));
			webhook.setDeleteAll(events.contains("deleteAll"));
			webhook.setCustomEvents(events.stream().filter(e -> !StringUtils.equalsAny(e,
					"create", "update", "delete", "createAll", "updateAll", "deleteAll")).collect(Collectors.toList()));
			if (utils.getCoreScooldTypes().contains(type)) {
				webhook.setTypeFilter(type);
			}
			webhook.setUrlEncoded(!json);
			webhook.setPropertyFilter(filter);
			webhook.resetSecret();
			pc.create(webhook);
		} else {
			model.addAttribute("error", Collections.singletonMap("targetUrl", utils.getLang(req).get("requiredfield")));
			return "base";
		}
		return "redirect:" + ADMINLINK + "#webhooks-tab";
	}

	@PostMapping("/toggle-webhook")
	public String toggleWebhook(@RequestParam String id, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(id) && utils.isAdmin(authUser) && utils.isWebhooksEnabled()) {
			Webhook webhook = pc.read(id);
			if (webhook != null) {
				webhook.setActive(!webhook.getActive());
				pc.update(webhook);
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "base";
		} else {
			return "redirect:" + ADMINLINK + "#webhooks-tab";
		}
	}

	@PostMapping("/delete-webhook")
	public String deleteWebhook(@RequestParam String id, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(id) && utils.isAdmin(authUser) && utils.isWebhooksEnabled()) {
			Webhook webhook = new Webhook();
			webhook.setId(id);
			pc.delete(webhook);
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "base";
		} else {
			return "redirect:" + ADMINLINK + "#webhooks-tab";
		}
	}

	@PostMapping
	public String forceDelete(@RequestParam Boolean confirmdelete, @RequestParam String id, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		if (confirmdelete && utils.isAdmin(authUser)) {
			ParaObject object = pc.read(id);
			if (object != null) {
				object.delete();
				logger.info("{} #{} deleted {} #{}", authUser.getName(), authUser.getId(),
						object.getClass().getName(), object.getId());
			}
		}
		return "redirect:" + Optional.ofNullable(req.getParameter("returnto")).orElse(ADMINLINK);
	}

	@GetMapping(value = "/export", produces = "application/zip")
	public ResponseEntity<StreamingResponseBody> backup(HttpServletRequest req, HttpServletResponse response) {
		Profile authUser = utils.getAuthUser(req);
		if (!utils.isAdmin(authUser) || !CONF.dataImportExportEnabled()) {
			return new ResponseEntity<StreamingResponseBody>(HttpStatus.FORBIDDEN);
		}
		String fileName = App.identifier(CONF.paraAccessKey()) + "_" + Utils.formatDate("YYYYMMdd_HHmmss", Locale.US);
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".zip");
		return new ResponseEntity<StreamingResponseBody>(out -> {
			// export all fields, even those which are JSON-ignored
			ObjectWriter writer = JsonMapper.builder().disable(MapperFeature.USE_ANNOTATIONS).build().writer().
					without(SerializationFeature.INDENT_OUTPUT).
					without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
				long count = 0;
				int partNum = 0;
				// find all objects even if there are more than 10000 users in the system
				Pager pager = new Pager(1, "_docid", false, CONF.maxItemsPerPage());
				List<ParaObject> objects;
				do {
					objects = pc.findQuery("", "*", pager);
					ZipEntry zipEntry = new ZipEntry(fileName + "_part" + (++partNum) + ".json");
					zipOut.putNextEntry(zipEntry);
					writer.writeValue(zipOut, objects);
					count += objects.size();
				} while (!objects.isEmpty());
				logger.info("Exported {} objects to {}. Downloaded by {} (pager.count={})", count, fileName + ".zip",
						authUser.getCreatorid() + " " + authUser.getName(), pager.getCount());
			} catch (final IOException e) {
				logger.error("Failed to export data.", e);
			}
		}, HttpStatus.OK);
	}

	@PostMapping("/import")
	public String restore(@RequestParam("file") MultipartFile file,
			@RequestParam(required = false, defaultValue = "false") Boolean isso,
			@RequestParam(required = false, defaultValue = "false") Boolean deleteall,
			HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!utils.isAdmin(authUser) || !CONF.dataImportExportEnabled()) {
			res.setStatus(HttpStatus.FORBIDDEN.value());
			return null;
		}
		ObjectReader reader = ParaObjectUtils.getJsonMapper().readerFor(new TypeReference<List<Map<String, Object>>>() { });
		String filename = file.getOriginalFilename();
		Sysprop s = new Sysprop();
		s.setType("scooldimport");
		s.setCreatorid(authUser.getCreatorid());
		s.setName(authUser.getName());
		s.addProperty("status", "pending");
		s.addProperty("count", 0);
		s.addProperty("file", filename);
		Sysprop si = pc.create(s);

		Para.asyncExecute(() -> {
			Map<String, String> comments2authors = new LinkedHashMap<>();
			Map<String, User> accounts2emails = new LinkedHashMap<>();
			try (InputStream inputStream = file.getInputStream()) {
				if (deleteall) {
					logger.info("Deleting all existing objects before import...");
					List<String> toDelete = new LinkedList<>();
					pc.readEverything((pager) -> {
						pager.setSelect(Collections.singletonList(Config._ID));
						List<Sysprop> objects = pc.findQuery("", "*", pager);
						toDelete.addAll(objects.stream().map(r -> r.getId()).collect(Collectors.toList()));
						return objects;
					});
					pc.deleteAll(toDelete);
				}
				if (StringUtils.endsWithIgnoreCase(filename, ".zip")) {
					try (ZipInputStream zipIn = new ZipInputStream(inputStream)) {
						ZipEntry zipEntry;
						List<ParaObject> toCreate = new LinkedList<ParaObject>();
						long countUpdated = Utils.timestamp();
						while ((zipEntry = zipIn.getNextEntry()) != null) {
							if (isso) {
								importFromSOArchive(zipIn, zipEntry, reader, comments2authors, accounts2emails, si);
							} else if (zipEntry.getName().endsWith(".json")) {
								List<Map<String, Object>> objects = reader.readValue(new FilterInputStream(zipIn) {
									public void close() throws IOException {
										zipIn.closeEntry();
									}
								});
								objects.forEach(o -> toCreate.add(ParaObjectUtils.setAnnotatedFields(o)));
								if (toCreate.size() >= CONF.importBatchSize()) {
									pc.createAll(toCreate);
									toCreate.clear();
								}
								si.addProperty("count", ((int) si.getProperty("count")) + objects.size());
							} else {
								logger.error("Expected JSON but found unknown file type to import: {}", zipEntry.getName());
							}
							if (Utils.timestamp() > countUpdated + TimeUnit.SECONDS.toMillis(5)) {
								pc.update(si);
								countUpdated = Utils.timestamp();
							}
						}
						if (!toCreate.isEmpty()) {
							pc.createAll(toCreate);
						}
						if (isso) {
							// apply additional fixes to data
							updateSOCommentAuthors(comments2authors);
							updateSOUserAccounts(accounts2emails);
						}
					}
				} else if (StringUtils.endsWithIgnoreCase(filename, ".json")) {
					if (isso) {
						List<Map<String, Object>> objs = reader.readValue(inputStream);
						importFromSOArchiveSingle(filename, objs, comments2authors, accounts2emails, si);
					} else {
						List<Map<String, Object>> objects = reader.readValue(inputStream);
						List<ParaObject> toCreate = new LinkedList<ParaObject>();
						objects.forEach(o -> toCreate.add(ParaObjectUtils.setAnnotatedFields(o)));
						si.addProperty("count", objects.size());
						pc.createAll(toCreate);
					}
				}
				logger.info("Imported {} objects to {}. Executed by {}", si.getProperty("count"),
						CONF.paraAccessKey(), authUser.getCreatorid() + " " + authUser.getName());
				si.addProperty("status", "done");
			} catch (Exception e) {
				logger.error("Failed to import " + filename, e);
				si.addProperty("status", "failed");
			} finally {
				pc.update(si);
			}
		});
		return "redirect:" + ADMINLINK + "?success=true&imported=1#backup-tab";
	}

	@PostMapping("/set-theme")
	public String setTheme(@RequestParam String theme, @RequestParam String css, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			utils.setCustomTheme(Utils.stripAndTrim(theme, "", true), css);
		}
		return "redirect:" + ADMINLINK + "#themes-tab";
	}

	@ResponseBody
	@PostMapping(path = "/generate-api-key", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> generateAPIKey(@RequestParam Integer validityHours,
			HttpServletRequest req, Model model) throws ParseException {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			String jti = UUID.randomUUID().toString();
			Map<String, Object> claims = Collections.singletonMap("jti", jti);
			SignedJWT jwt = utils.generateJWToken(claims, TimeUnit.HOURS.toSeconds(validityHours));
			if (jwt != null) {
				String jwtString = jwt.serialize();
				Date exp = jwt.getJWTClaimsSet().getExpirationTime();
				utils.registerApiKey(jti, jwtString);
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("jti", jti);
				data.put("jwt", jwtString);
				data.put("exp", exp == null ? 0L : Utils.formatDate(exp.getTime(), "YYYY-MM-dd HH:mm", Locale.UK));
				return ResponseEntity.ok().body(data);
			}
		}
		return ResponseEntity.status(403).build();
	}

	@ResponseBody
	@PostMapping(path = "/revoke-api-key", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> revokeAPIKey(@RequestParam String jti, HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			utils.revokeApiKey(jti);
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.status(403).build();
	}

	@PostMapping("/reindex")
	public String reindex(HttpServletRequest req, Model model) {
		if (utils.isAdmin(utils.getAuthUser(req))) {
			Para.asyncExecute(() -> pc.rebuildIndex());
			logger.info("Started rebuilding the search index for '{}'...", CONF.paraAccessKey());
		}
		return "redirect:" + ADMINLINK;
	}

	@PostMapping("/save-config")
	public String saveConfig(@RequestParam String key, @RequestParam(defaultValue = "") String value,
			HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser) && CONF.configEditingEnabled()) {
			if ("on".equals(value)) {
				value = "true";
			} else if ("true".equals(req.getParameter("isbool")) && StringUtils.isBlank(value)) {
				value = "false";
			}
			com.typesafe.config.Config modifiedConf = CONF.getConfig();
			if (value != null && !StringUtils.isBlank(value)) {
				modifiedConf = modifiedConf.withValue(key, ConfigValueFactory.fromAnyRef(value));
				System.setProperty(key, value);
			} else {
				modifiedConf = modifiedConf.withoutPath(key);
				System.clearProperty(key);
			}
			logger.info("Configuration property '{}' was modified by user {}.", key, authUser.getCreatorid());
			CONF.overwriteConfig(modifiedConf).store();
			if (CONF.getParaAppSettings().containsKey(key)) {
				pc.addAppSetting(key, value);
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "base";
		} else {
			return "redirect:" + ADMINLINK + "#configuration-tab";
		}
	}

	private List<ParaObject> importFromSOArchive(ZipInputStream zipIn, ZipEntry zipEntry, ObjectReader mapReader,
			Map<String, String> comments2authors, Map<String, User> accounts2emails, Sysprop si)
			throws IOException, ParseException {
		if (zipEntry.getName().endsWith(".json")) {
			List<Map<String, Object>> objs = mapReader.readValue(new FilterInputStream(zipIn) {
				public void close() throws IOException {
					zipIn.closeEntry();
				}
			});
			// IN PRO: rewrite all image links to relative local URLs
			return importFromSOArchiveSingle(zipEntry.getName(), objs, comments2authors, accounts2emails, si);
		} else {
			// IN PRO: store files in ./uploads
			return Collections.emptyList();
		}
	}

	private List<ParaObject> importFromSOArchiveSingle(String fileName, List<Map<String, Object>> objs,
			Map<String, String> comments2authors, Map<String, User> accounts2emails, Sysprop si) throws ParseException {
		List<ParaObject> toImport = new LinkedList<>();
		switch (fileName) {
			case "posts.json":
				importPostsFromSO(objs, toImport, si);
				break;
			case "tags.json":
				importTagsFromSO(objs, toImport, si);
				break;
			case "comments.json":
				importCommentsFromSO(objs, toImport, comments2authors, si);
				break;
			case "users.json":
				importUsersFromSO(objs, toImport, accounts2emails, si);
				break;
			case "users2badges.json":
				// nice to have...
				break;
			case "accounts.json":
				importAccountsFromSO(objs, accounts2emails);
				break;
			default:
				break;
		}
		return toImport;
	}

	private void importPostsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport, Sysprop si)
			throws ParseException {
		logger.info("Importing {} posts...", objs.size());
		int imported = 0;
		for (Map<String, Object> obj : objs) {
			Post p;
			if (StringUtils.equalsAnyIgnoreCase((String) obj.get("postType"), "question", "article")) {
				p = new Question();
				p.setTitle((String) obj.get("title"));
				String t = StringUtils.trimToEmpty(StringUtils.stripStart(StringUtils.stripEnd((String) obj.
						getOrDefault("tags", ""), "|"), "|"));
				p.setTags(Arrays.asList(t.split("\\|")));
				p.setAnswercount(((Integer) obj.getOrDefault("answerCount", 0)).longValue());
				p.setViewcount(((Integer) obj.getOrDefault("viewCount", 0)).longValue());
				Integer answerId = (Integer) obj.getOrDefault("acceptedAnswerId", null);
				p.setAnswerid(answerId != null ? "post_" + answerId : null);
			} else if ("answer".equalsIgnoreCase((String) obj.get("postType"))) {
				p = new Reply();
				Integer parentId = (Integer) obj.getOrDefault("parentId", null);
				p.setParentid(parentId != null ? "post_" + parentId : null);
			} else {
				continue;
			}
			p.setId("post_" + (Integer) obj.getOrDefault("id", Utils.getNewId()));
			p.setBody((String) obj.get("bodyMarkdown"));
			p.setSpace((String) obj.getOrDefault("space", Post.DEFAULT_SPACE)); // optional
			p.setVotes((Integer) obj.getOrDefault("score", 0));
			p.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat1, soDateFormat2).getTime());
			Integer creatorId = (Integer) obj.getOrDefault("ownerUserId", null);
			if (creatorId == null || creatorId == -1) {
				p.setCreatorid(utils.getSystemUser().getId());
			} else {
				p.setCreatorid(Profile.id("user_" + creatorId)); // add prefix to avoid conflicts
			}
			toImport.add(p);
			imported++;
			if (toImport.size() >= CONF.importBatchSize()) {
				pc.createAll(toImport);
				toImport.clear();
			}
		}
		if (!toImport.isEmpty()) {
			pc.createAll(toImport);
			toImport.clear();
		}
		si.addProperty("count", ((int) si.getProperty("count")) + imported);
	}

	private void importTagsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport, Sysprop si) {
		logger.info("Importing {} tags...", objs.size());
		int imported = 0;
		for (Map<String, Object> obj : objs) {
			Tag t = new Tag((String) obj.get("name"));
			t.setCount((Integer) obj.getOrDefault("count", 0));
			toImport.add(t);
			imported++;
			if (toImport.size() >= CONF.importBatchSize()) {
				pc.createAll(toImport);
				toImport.clear();
			}
		}
		if (!toImport.isEmpty()) {
			pc.createAll(toImport);
			toImport.clear();
		}
		si.addProperty("count", ((int) si.getProperty("count")) + imported);
	}

	private void importCommentsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport,
			Map<String, String> comments2authors, Sysprop si) throws ParseException {
		logger.info("Importing {} comments...", objs.size());
		int imported = 0;
		for (Map<String, Object> obj : objs) {
			Comment c = new Comment();
			c.setId("comment_" + (Integer) obj.get("id"));
			c.setComment((String) obj.get("text"));
			Integer parentId = (Integer) obj.getOrDefault("postId", null);
			c.setParentid(parentId != null ? "post_" + parentId : null);
			c.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat1, soDateFormat2).getTime());
			Integer creatorId = (Integer) obj.getOrDefault("userId", null);
			String userid = "user_" + creatorId;
			c.setCreatorid(creatorId != null ? Profile.id(userid) : utils.getSystemUser().getId());
			comments2authors.put(c.getId(), userid);
			toImport.add(c);
			imported++;
			if (toImport.size() >= CONF.importBatchSize()) {
				pc.createAll(toImport);
				toImport.clear();
			}
		}
		if (!toImport.isEmpty()) {
			pc.createAll(toImport);
			toImport.clear();
		}
		si.addProperty("count", ((int) si.getProperty("count")) + imported);
	}

	@SuppressWarnings("unchecked")
	private void importUsersFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport,
			Map<String, User> accounts2emails, Sysprop si) throws ParseException {
		logger.info("Importing {} users...", objs.size());
		int imported = 0;
		for (Map<String, Object> obj : objs) {
			User u = new User();
			u.setId("user_" + (Integer) obj.get("id"));
			u.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat1, soDateFormat2).getTime());
			u.setActive(true);
			u.setCreatorid(((Integer) obj.get("accountId")).toString());
			u.setGroups("admin".equalsIgnoreCase((String) obj.get("userTypeId"))
					? User.Groups.ADMINS.toString() :
					("mod".equalsIgnoreCase((String) obj.get("userTypeId")) ?
							User.Groups.MODS.toString() : User.Groups.USERS.toString()));
			u.setEmail(u.getId() + "@scoold.com");
			u.setIdentifier(u.getEmail());
			u.setName((String) obj.get("realName"));
			String lastLogin = (String) obj.get("lastLoginDate");
			u.setUpdated(StringUtils.isBlank(lastLogin) ? null :
					DateUtils.parseDate(lastLogin, soDateFormat1, soDateFormat2).getTime());
			u.setPicture((String) obj.get("profileImageUrl"));

			Sysprop s = new Sysprop();
			s.setId(u.getIdentifier());
			s.setName(Config._IDENTIFIER);
			s.setCreatorid(u.getId());
			String password = (String) obj.getOrDefault("passwordHash", Utils.bcrypt(Utils.generateSecurityToken(10)));
			if (!StringUtils.isBlank(password)) {
				s.addProperty(Config._PASSWORD, password);
				u.setPassword(password);
			}

			Profile p = Profile.fromUser(u);
			p.setVotes((Integer) obj.get("reputation"));
			p.setAboutme((String) obj.getOrDefault("title", ""));
			p.setLastseen(u.getUpdated());
			p.setSpaces(new HashSet<String>((List<String>) obj.getOrDefault("spaces", List.of(Post.DEFAULT_SPACE))));
			toImport.add(u);
			toImport.add(p);
			toImport.add(s);
			imported += 2;

			User cachedUser = accounts2emails.get(u.getCreatorid());
			if (cachedUser == null) {
				User cu = new User(u.getId());
				accounts2emails.put(u.getCreatorid(), cu);
			} else {
				cachedUser.setId(u.getId());
			}

			if (toImport.size() >= CONF.importBatchSize()) {
				pc.createAll(toImport);
				toImport.clear();
			}
		}
		if (!toImport.isEmpty()) {
			pc.createAll(toImport);
			toImport.clear();
		}
		si.addProperty("count", ((int) si.getProperty("count")) + imported);
	}

	private void importAccountsFromSO(List<Map<String, Object>> objs, Map<String, User> accounts2emails) {
		logger.info("Importing {} accounts...", objs.size());
		for (Map<String, Object> obj : objs) {
			String accountId = ((Integer) obj.get("accountId")).toString();
			String email = (String) obj.get("verifiedEmail");
			User cachedUser = accounts2emails.get(accountId);
			if (cachedUser == null) {
				User cu = new User();
				cu.setEmail(email);
				cu.setIdentifier(email);
				accounts2emails.put(accountId, cu);
			} else {
				cachedUser.setEmail(email);
				cachedUser.setIdentifier(email);
			}
		}
	}

	private void updateSOCommentAuthors(Map<String, String> comments2authors) {
		if (!comments2authors.isEmpty()) {
			// fetch & update comment author names
			Map<String, ParaObject> authors = pc.readAll(new ArrayList<>(comments2authors.values())).stream().
					collect(Collectors.toMap(k -> k.getId(), v -> v));
			List<Map<String, String>> toPatch = new LinkedList<>();
			for (Map.Entry<String, String> entry : comments2authors.entrySet()) {
				Map<String, String> user = new HashMap<>();
				user.put(Config._ID, entry.getKey());
				if (authors.containsKey(entry.getValue())) {
					user.put("authorName", authors.get(entry.getValue()).getName());
				}
				toPatch.add(user);
				if (toPatch.size() >= CONF.importBatchSize()) {
					pc.invokePatch("_batch", toPatch, Map.class);
					toPatch.clear();
				}
			}
			if (!toPatch.isEmpty()) {
				pc.invokePatch("_batch", toPatch, Map.class);
				toPatch.clear();
			}
		}
	}

	private void updateSOUserAccounts(Map<String, User> accounts2emails) {
		List<Map<String, String>> toPatch = new LinkedList<>();
		for (Map.Entry<String, User> entry : accounts2emails.entrySet()) {
			User u = entry.getValue();
			Map<String, String> user = new HashMap<>();
			user.put(Config._ID, u.getId());
			user.put(Config._EMAIL, u.getEmail());
			user.put(Config._IDENTIFIER, u.getEmail());
			toPatch.add(user);
			if (toPatch.size() >= CONF.importBatchSize()) {
				pc.invokePatch("_batch", toPatch, Map.class);
				toPatch.clear();
			}
		}
		if (!toPatch.isEmpty()) {
			pc.invokePatch("_batch", toPatch, Map.class);
			toPatch.clear();
		}
	}

	private List<Sysprop> getSpaces(Pager itemcount) {
		Set<Sysprop> spaces = utils.getAllSpacesAdmin();
		itemcount.setCount(spaces.size());
		LinkedList<Sysprop> list = new LinkedList<>(spaces.stream().
				filter(s -> !utils.isDefaultSpace(s.getName())).collect(Collectors.toList()));
		if (itemcount.getPage() <= 1) {
			list.addFirst(utils.buildSpaceObject("default"));
		}
		return list;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.RateLimiter;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.REPORTSLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.REPORTER;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/reports")
public class ReportsController {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final RateLimiter reportsLimiter;
	private final RateLimiter reportsLimiterAnon;

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	private QuestionController questionController;

	@Inject
	public ReportsController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		this.reportsLimiter = Para.createRateLimiter(3, 10, 20);
		this.reportsLimiterAnon = Para.createRateLimiter(1, 3, 5);
	}

	@GetMapping({"", "/delete-all"})
	public String get(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req) && !utils.isMod(utils.getAuthUser(req))) {
			return "redirect:" + HOMEPAGE;
		} else if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + REPORTSLINK;
		}
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);
		List<Report> reportslist = pc.findQuery(Utils.type(Report.class), "*", itemcount);
		model.addAttribute("path", "reports.vm");
		model.addAttribute("title", utils.getLang(req).get("reports.title"));
		model.addAttribute("reportsSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("reportslist", reportslist);
		Pager count = new Pager(1);
		pc.findQuery("", "type:(" + Utils.type(UnapprovedQuestion.class) + " OR " + Utils.type(UnapprovedReply.class) + ")", count);
		model.addAttribute("unapprovedCount", count.getCount());
		return "base";
	}

	@GetMapping("/form")
	public String getReportForm(@RequestParam String parentid, @RequestParam String type,
			@RequestParam(required = false) String link, Model model) {
		model.addAttribute("getreportform", true);
		model.addAttribute("parentid", parentid);
		model.addAttribute("type", type);
		model.addAttribute("link", link);
		return "reports";
	}

	@PostMapping
	public void create(HttpServletRequest req, HttpServletResponse res, Model model) {
		Report rep = utils.populate(req, new Report(), "link", "description", "parentid", "subType", "authorName");
		Map<String, String> error = utils.validate(rep);
		if (error.isEmpty()) {
			boolean canCreateReport;
			if (utils.isAuthenticated(req)) {
				Profile authUser = utils.getAuthUser(req);
				rep.setAuthorName(authUser.getName());
				rep.setCreatorid(authUser.getId());
				canCreateReport = reportsLimiter.isAllowed(utils.getParaAppId(), authUser.getCreatorid());
				utils.addBadgeAndUpdate(authUser, REPORTER, canCreateReport);
			} else {
				//allow anonymous reports
				rep.setAuthorName(utils.getLang(req).get("anonymous"));
				canCreateReport = reportsLimiterAnon.isAllowed(utils.getParaAppId(), req.getRemoteAddr());
			}
			if (StringUtils.startsWith(rep.getLink(), "/")) {
				rep.setLink(CONF.serverUrl() + rep.getLink());
			}
			if (canCreateReport) {
				rep.create();
				model.addAttribute("newreport", rep);
				res.setStatus(200);
			} else {
				model.addAttribute("error", "Too many requests.");
				res.setStatus(400);
			}
		} else {
			model.addAttribute("error", error);
			res.setStatus(400);
		}
	}

	@PostMapping("/cspv")
	@SuppressWarnings("unchecked")
	public void createCSPViolationReport(HttpServletRequest req, HttpServletResponse res) throws IOException {
		if (CONF.cspReportsEnabled()) {
			Report rep = new Report();
			rep.setDescription("CSP Violation Report");
			rep.setSubType(Report.ReportType.OTHER);
			rep.setLink("-");
			rep.setAuthorName(CONF.appName());
			Map<String, Object> body = ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
			if (body != null && !body.isEmpty()) {
				rep.setProperties((Map<String, Object>) (body.containsKey("csp-report") ? body.get("csp-report") : body));
				if (rep.getProperties().containsKey("document-uri")) {
					rep.setLink((String) rep.getProperties().get("document-uri"));
				} else if (rep.getProperties().containsKey("source-file")) {
					rep.setLink((String) rep.getProperties().get("source-file"));
				}
				body.remove("original-policy");
				body.put("userAgent", req.getHeader("User-Agent") + "");
				body.put("userHost", req.getRemoteHost() + "");
			}
			rep.create();
			res.setStatus(200);
		} else {
			res.setStatus(403);
		}
	}

	@PostMapping("/{id}/close")
	public String close(@PathVariable String id, @RequestParam(required = false, defaultValue = "") String solution,
			HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Report report = pc.read(id);
			if (report != null && !report.getClosed() && utils.isMod(authUser)) {
				report.setClosed(true);
				report.setSolution(solution);
				report.update();
			}
		}
		if (!utils.isAjaxRequest(req)) {
			return "redirect:" + REPORTSLINK;
		}
		return "base";
	}

	@PostMapping("/{id}/open")
	public String open(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Report report = pc.read(id);
			if (report != null && utils.isMod(authUser)) {
				report.setClosed(false);
				report.update();
			}
		}
		if (!utils.isAjaxRequest(req)) {
			return "redirect:" + REPORTSLINK;
		}
		return "base";
	}

	@PostMapping("/{id}/approve")
	public String approveItem(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Report report = pc.read(id);
		if (report != null) {
			questionController.modApprove(report.getParentid(), req);
			report.setClosed(true);
			report.setDescription(report.getDescription() + " ");
			report.delete();
		}
		if (!utils.isAjaxRequest(req)) {
			return "redirect:" + REPORTSLINK;
		}
		return "base";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Report rep = pc.read(id);
			if (rep != null && utils.isAdmin(authUser)) {
				rep.delete();
			}
		}
		if (!utils.isAjaxRequest(req)) {
			return "redirect:" + REPORTSLINK;
		}
		return "base";
	}

	@PostMapping("/{id}/confirm-spam")
	public String confirmSpam(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req) && !StringUtils.isBlank(CONF.akismetApiKey())) {
			Profile authUser = utils.getAuthUser(req);
			Report rep = pc.read(id);
			if (rep != null && utils.isAdmin(authUser)) {
				utils.confirmSpam(utils.buildAkismetCommentFromReport(rep, req),
						"true".equals(req.getParameter("spam")), true, req);

				if ("true".equals(req.getParameter("deleteUser"))) {
					Profile p = pc.read(rep.getCreatorid());
					if (p != null && !utils.isMod(p)) {
						p.delete();
					}
				}
				rep.delete();
			}
		}
		if (!utils.isAjaxRequest(req)) {
			return "redirect:" + REPORTSLINK;
		}
		return "base";
	}

	@PostMapping("/delete-all")
	public String deleteAll(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			if (utils.isAdmin(authUser)) {
				List<String> toDelete = new LinkedList<>();
				pc.readEverything(pager -> {
					//pager.setSelect(Collections.singletonList(Config._ID));
					List<ParaObject> reports = pc.findQuery(Utils.type(Report.class), "*", pager);
					toDelete.addAll(reports.stream().map(r -> r.getId()).collect(Collectors.toList()));
					return reports;
				});
				pc.deleteAll(toDelete);
			}
		}
		return "redirect:" + REPORTSLINK;
	}

	@PostMapping("/cleanup-unapproved")
	public String cleanupUnapproved(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			if (utils.isAdmin(authUser)) {
				List<String> toDelete = new LinkedList<>();
				pc.readEverything(pager -> {
					//pager.setSelect(Collections.singletonList(Config._ID));
					List<ParaObject> objects = pc.findQuery("", Config._TYPE + ":" + Utils.type(UnapprovedQuestion.class) +
							" OR " + Config._TYPE + ":" + Utils.type(UnapprovedReply.class), pager);
					toDelete.addAll(objects.stream().map(r -> r.getId()).collect(Collectors.toList()));
					return objects;
				});
				pc.deleteAll(toDelete);
			}
		}
		return "redirect:" + REPORTSLINK;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.Sysprop;
import com.erudika.para.core.utils.Para;
import static com.erudika.scoold.ScooldServer.TERMSLINK;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/terms")
public class TermsController {

	private final ScooldUtils utils;

	@Inject
	public TermsController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		model.addAttribute("path", "terms.vm");
		model.addAttribute("title", utils.getLang(req).get("terms.title"));
		model.addAttribute("termshtml", utils.getParaClient().read("template" + Para.getConfig().separator() + "terms"));
		return "base";
	}

	@PostMapping
	public String edit(@RequestParam String termshtml, HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req) || !utils.isAdmin(utils.getAuthUser(req))) {
			return "redirect:" + TERMSLINK;
		}
		Sysprop terms = new Sysprop("template" + Para.getConfig().separator() + "terms");
		if (StringUtils.isBlank(termshtml)) {
			utils.getParaClient().delete(terms);
		} else {
			terms.addProperty("html", termshtml);
			utils.getParaClient().create(terms);
		}
		return "redirect:" + TERMSLINK;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.Sysprop;
import com.erudika.para.core.utils.Para;
import static com.erudika.scoold.ScooldServer.ABOUTLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/about")
public class AboutController {

	private final ScooldUtils utils;

	@Inject
	public AboutController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		model.addAttribute("path", "about.vm");
		model.addAttribute("title", utils.getLang(req).get("about.title"));
		model.addAttribute("abouthtml", utils.getParaClient().read("template" + Para.getConfig().separator() + "about"));

		model.addAttribute("NICEPROFILE_BONUS", Profile.Badge.NICEPROFILE.getReward());
		model.addAttribute("SUPPORTER_BONUS", Profile.Badge.SUPPORTER.getReward());
		model.addAttribute("NOOB_BONUS", Profile.Badge.NOOB.getReward());
		model.addAttribute("GOODQUESTION_BONUS", Profile.Badge.GOODQUESTION.getReward());
		model.addAttribute("GOODANSWER_BONUS", Profile.Badge.GOODANSWER.getReward());
		return "base";
	}

	@PostMapping
	public String edit(@RequestParam String abouthtml, HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req) || !utils.isAdmin(utils.getAuthUser(req))) {
			return "redirect:" + ABOUTLINK;
		}
		Sysprop about = new Sysprop("template" + Para.getConfig().separator() + "about");
		if (StringUtils.isBlank(abouthtml)) {
			utils.getParaClient().delete(about);
		} else {
			about.addProperty("html", abouthtml);
			utils.getParaClient().create(about);
		}
		return "redirect:" + ABOUTLINK;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.FEEDBACKLINK;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/feedback")
public class FeedbackController {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public FeedbackController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public String get(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			HttpServletRequest req, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + FEEDBACKLINK;
		}
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);
		List<Post> feedbacklist = pc.findQuery(Utils.type(Feedback.class), "*", itemcount);
		utils.getProfiles(feedbacklist);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title"));
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("feedbacklist", feedbacklist);
		return "base";
	}

	@GetMapping({"/{id}", "/{id}/{title}"})
	public String getById(@PathVariable String id, @PathVariable(required = false) String title,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + FEEDBACKLINK;
		}
		Feedback showPost = pc.read(id);
		if (showPost == null) {
			return "redirect:" + FEEDBACKLINK;
		}
		Pager itemcount = utils.getPager("page", req);
		List<Reply> answerslist = showPost.getAnswers(itemcount);
		LinkedList<Post> allPosts = new LinkedList<Post>();
		allPosts.add(showPost);
		allPosts.addAll(answerslist);
		utils.getProfiles(allPosts);
		utils.getComments(allPosts);
		utils.updateViewCount(showPost, req, res);

		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title") + " - " + showPost.getTitle());
		model.addAttribute("description", Utils.abbreviate(Utils.stripAndTrim(showPost.getBody(), " "), 195));
		model.addAttribute("showPost", allPosts.removeFirst());
		model.addAttribute("answerslist", allPosts);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("includeEmojiPicker", true);
		return "base";
	}

	@GetMapping("/write")
	public String write(HttpServletRequest req, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + FEEDBACKLINK + "/write";
		}
		model.addAttribute("write", true);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title") + " - "
				+ utils.getLang(req).get("feedback.write"));
		model.addAttribute("includeEmojiPicker", true);
		return "base";
	}

	@GetMapping("/tag/{tag}")
	public String getTagged(@PathVariable String tag, HttpServletRequest req, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + FEEDBACKLINK;
		}
		Pager itemcount = utils.getPager("page", req);
		List<Post> feedbacklist = pc.findTagged(Utils.type(Feedback.class), new String[]{tag}, itemcount);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title"));
		model.addAttribute("tag", tag);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("feedbacklist", feedbacklist);
		return "base";
	}

	@PostMapping
	public String createAjax(HttpServletRequest req, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		model.addAttribute("path", "feedback.vm");
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Post post = utils.populate(req, new Feedback(), "title", "body", "tags|,");
			Map<String, String> error = utils.validate(post);
			handleSpam(post, authUser, error, req);
			if (authUser != null && error.isEmpty()) {
				post.setCreatorid(authUser.getId());
				post.create();
				authUser.setLastseen(System.currentTimeMillis());
				return "redirect:" + FEEDBACKLINK;
			} else {
				model.addAttribute("error", error);
				model.addAttribute("write", true);
				return "base";
			}
		}
		return "redirect:" + FEEDBACKLINK;
	}

	@PostMapping({"/{id}", "/{id}/{title}"})
	public String replyAjax(@PathVariable String id, @PathVariable(required = false) String title,
			HttpServletRequest req, HttpServletResponse res, Model model) throws IOException {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (authUser != null && showPost != null && !showPost.isClosed() && !showPost.isReply()) {
			//create new answer
			Reply answer = utils.populate(req, new Reply(), "body");
			Map<String, String> error = utils.validate(answer);
			handleSpam(answer, authUser, error, req);
			if (!error.containsKey("body")) {
				answer.setTitle(showPost.getTitle());
				answer.setCreatorid(authUser.getId());
				answer.setParentid(showPost.getId());
				answer.create();

				showPost.setAnswercount(showPost.getAnswercount() + 1);
				if (showPost.getAnswercount() >= CONF.maxRepliesPerPost()) {
					showPost.setCloserid("0");
				}
				// update without adding revisions
				pc.update(showPost);
				utils.addBadgeAndUpdate(authUser, Profile.Badge.EUREKA, answer.getCreatorid().equals(showPost.getCreatorid()));
				answer.setAuthor(authUser);
				model.addAttribute("showPost", showPost);
				model.addAttribute("answerslist", Collections.singletonList(answer));
			} else {
				model.addAttribute("error", error);
				model.addAttribute("path", "feedback.vm");
				res.setStatus(400);
			}
			return "reply";
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "reply";
		} else {
			return "redirect:" + FEEDBACKLINK + "/" + id;
		}
	}

	@PostMapping("/{id}/delete")
	public String deleteAjax(@PathVariable String id, HttpServletRequest req) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		if (utils.isAuthenticated(req)) {
			Feedback showPost = pc.read(id);
			if (showPost != null) {
				showPost.delete();
			}
		}
		return "redirect:" + FEEDBACKLINK;
	}

	private void handleSpam(Post q, Profile authUser, Map<String, String> error, HttpServletRequest req) {
		boolean isSpam = utils.isSpam(q, authUser, req);
		if (isSpam && CONF.automaticSpamProtectionEnabled()) {
			error.put("body", "spam");
			Report rep = new Report();
			rep.setName(q.getTitle());
			rep.setContent(Utils.abbreviate(Utils.markdownToHtml(q.getBody()), 2000));
			rep.setCreatorid(authUser.getId());
			rep.setParentid(q.getId());
			rep.setDescription("SPAM detected");
			rep.setSubType(Report.ReportType.SPAM);
			rep.setLink(q.getPostLinkForRedirect());
			rep.setAuthorName(authUser.getName());
			rep.create();
		}
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.SETTINGSLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.utils.avatars.AvatarRepository;
import com.erudika.scoold.utils.avatars.AvatarRepositoryProxy;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.logicsquad.qr4j.QrCode;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

	private final ScooldUtils utils;
	private final ParaClient pc;
	private final AvatarRepository avatarRepository;
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	@Inject
	public SettingsController(ScooldUtils utils, AvatarRepositoryProxy avatarRepository) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		this.avatarRepository = avatarRepository;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + SETTINGSLINK;
		}
		model.addAttribute("path", "settings.vm");
		model.addAttribute("title", utils.getLang(req).get("settings.title"));
		model.addAttribute("newpostEmailsEnabled", utils.isSubscribedToNewPosts(req));
		model.addAttribute("newreplyEmailsEnabled", utils.isSubscribedToNewReplies(req));
		model.addAttribute("emailsAllowed", utils.isNotificationsAllowed());
		model.addAttribute("newpostEmailsAllowed", utils.isNewPostNotificationAllowed());
		model.addAttribute("favtagsEmailsAllowed", utils.isFavTagsNotificationAllowed());
		model.addAttribute("replyEmailsAllowed", utils.isReplyNotificationAllowed());
		model.addAttribute("commentEmailsAllowed", utils.isCommentNotificationAllowed());
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		return "base";
	}

	@PostMapping
	public String post(@RequestParam(required = false) String tags, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String replyEmailsOn, @RequestParam(required = false) String commentEmailsOn,
			@RequestParam(required = false) String oldpassword, @RequestParam(required = false) String newpassword,
			@RequestParam(required = false) String newpostEmailsOn, @RequestParam(required = false) String favtagsEmailsOn,
			@RequestParam(required = false) List<String> favspaces, @RequestParam(required = false) String newreplyEmailsOn,
			HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			setFavTags(authUser, tags);
			setFavSpaces(authUser, favspaces);
			if (!StringUtils.isBlank(latlng)) {
				authUser.setLatlng(latlng);
			}
			setAnonymity(authUser, req.getParameter("anon"));
			setDarkMode(authUser, req.getParameter("dark"));
			authUser.setPreferredSpace(req.getParameter("preferredSpace"));
			authUser.setReplyEmailsEnabled(Boolean.valueOf(replyEmailsOn) && utils.isReplyNotificationAllowed());
			authUser.setCommentEmailsEnabled(Boolean.valueOf(commentEmailsOn) && utils.isCommentNotificationAllowed());
			authUser.setFavtagsEmailsEnabled(Boolean.valueOf(favtagsEmailsOn) && utils.isFavTagsNotificationAllowed());
			authUser.update();

			if (Boolean.valueOf(newpostEmailsOn) && utils.isNewPostNotificationAllowed()) {
				utils.subscribeToNewPosts(authUser.getUser());
			} else {
				utils.unsubscribeFromNewPosts(authUser.getUser());
			}
			if ("on".equals(newreplyEmailsOn) && utils.isReplyNotificationAllowed() && utils.isMod(authUser)) {
				utils.subscribeToNewReplies(authUser.getUser());
			} else {
				utils.unsubscribeFromNewReplies(authUser.getUser());
			}

			if (resetPasswordAndUpdate(authUser.getUser(), oldpassword, newpassword)) {
				utils.clearSession(req, res);
				return "redirect:" + SETTINGSLINK + "?passChanged=true";
			}
		}
		return "redirect:" + SETTINGSLINK;
	}

	@PostMapping("/goodbye")
	public String deleteAccount(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			utils.getAuthUser(req).delete();
			utils.clearSession(req, res);
		}
		return "redirect:" + CONF.signoutUrl(4);
	}

	@PostMapping("/toggle-twofa")
	public String toggle2FA(@RequestParam String code, @RequestParam(required = false, defaultValue = "") String backupCode,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (utils.isAuthenticated(req)) {
			String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
			User user = pc.me(jwt);
			if (user != null && (!StringUtils.isBlank(code) || !StringUtils.isBlank(backupCode))) {
				if (utils.isValid2FACode(user.getTwoFAkey(), NumberUtils.toInt(code, 0), 0) ||
						Utils.bcryptMatches(backupCode, user.getTwoFAbackupKeyHash())) {
					user.setTwoFA(!user.getTwoFA());
					Date issueTime = utils.getUnverifiedClaimsFromJWT(jwt).getIssueTime();
					if (user.getTwoFA()) {
						String backup = Utils.generateSecurityToken(20, true);
						user.setTwoFAbackupKeyHash(Utils.bcrypt(backup));
						model.addAttribute("backupCode", backup);
						HttpUtils.set2FACookie(user, issueTime, req, res);
					} else {
						user.setTwoFAkey("");
						user.setTwoFAbackupKeyHash("");
						HttpUtils.set2FACookie(null, null, req, res);
					}
					pc.update(user);
					utils.getAuthUser(req).setUser(user);
					return get(req, model);
				}
				return "redirect:" + SETTINGSLINK + "?code=signin.invalidcode&error=true";
			}
		}
		return "redirect:" + SETTINGSLINK;
	}

	@PostMapping("/reset-2fa")
	public String reset2FA(HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isAuthenticated(req)) {
			return null;
		}
		return "redirect:" + SETTINGSLINK;
	}

	@GetMapping(path = "/qr", produces = "image/svg+xml")
	public void generate2FAQRCode(HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isAuthenticated(req)) {
			return;
		}
		String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
		User user = pc.me(jwt);
		if (user == null) {
			return;
		}
		if (StringUtils.isBlank(user.getTwoFAkey())) {
			user.setTwoFAkey(Utils.generateSecurityToken(32, true));
			pc.update(user);
		}
		String otpProtocol = Utils.formatMessage("otpauth://totp/" + CONF.appName() + ":{0}?secret={1}&issuer=Scoold",
				user.getEmail(), new Base32().encodeAsString(user.getTwoFAkey().
				replaceAll("=", "").getBytes()).replaceAll("=", ""));

		try {
			QrCode qr = QrCode.encodeText(otpProtocol, QrCode.Ecc.MEDIUM);
			res.setContentType("image/svg+xml");
			res.getOutputStream().write(qr.toSvg(1, "#FFF", "#000").getBytes());
			res.getOutputStream().flush();
		} catch (Exception ex) {
			return;
		}
	}

	private boolean resetPasswordAndUpdate(User u, String pass, String newpass) {
		if (u != null && !StringUtils.isBlank(pass) && !StringUtils.isBlank(newpass) &&
				u.getIdentityProvider().equals("generic")) {
			Sysprop s = pc.read(u.getEmail());
			if (s != null && Utils.bcryptMatches(pass, (String) s.getProperty(Config._PASSWORD))) {
				String hashed = Utils.bcrypt(newpass);
				s.addProperty(Config._PASSWORD, hashed);
				u.setPassword(hashed);
				pc.update(s);
				return true;
			}
		}
		return false;
	}

	private void setFavTags(Profile authUser, String tags) {
		if (!StringUtils.isBlank(tags)) {
			Set<String> ts = new LinkedHashSet<String>();
			for (String tag : tags.split(",")) {
				if (!StringUtils.isBlank(tag) && ts.size() <= CONF.maxFavoriteTags()) {
					ts.add(tag);
				}
			}
			authUser.setFavtags(new LinkedList<String>(ts));
		} else {
			authUser.setFavtags(null);
		}
	}

	private void setFavSpaces(Profile authUser, List<String> spaces) {
		authUser.setFavspaces(null);
		if (spaces != null && !spaces.isEmpty()) {
			for (String space : spaces) {
				String spaceId = utils.getSpaceId(space);
				if (!StringUtils.isBlank(spaceId) && utils.canAccessSpace(authUser, spaceId)) {
					authUser.getFavspaces().add(spaceId);
				}
			}
		}
	}

	private void setAnonymity(Profile authUser, String anonParam) {
		if (utils.isAnonymityEnabled()) {
			if ("true".equalsIgnoreCase(anonParam)) {
				anonymizeProfile(authUser);
			} else if (authUser.getAnonymityEnabled()) {
				deanonymizeProfile(authUser);
			}
		}
	}

	private void setDarkMode(Profile authUser, String darkParam) {
		if (utils.isDarkModeEnabled()) {
			authUser.setDarkmodeEnabled("true".equalsIgnoreCase(darkParam));
			pc.update(authUser);
		}
	}

	private void anonymizeProfile(Profile authUser) {
		authUser.setName("Anonymous");
		authUser.setOriginalPicture(authUser.getPicture());
		authUser.setPicture(avatarRepository.getAnonymizedLink(authUser.getId() + "@scooldemail.com"));
		authUser.setAnonymityEnabled(true);
	}

	private void deanonymizeProfile(Profile authUser) {
		authUser.setName(authUser.getOriginalName());
		authUser.setPicture(authUser.getOriginalPicture());
		authUser.setAnonymityEnabled(false);
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/not-found")
public class NotFoundController {

	private final ScooldUtils utils;

	@Inject
	public NotFoundController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(HttpServletRequest req, HttpServletResponse res, Model model) {
		model.addAttribute("path", "notfound.vm");
		model.addAttribute("title", utils.getLang(req).get("notfound.title"));
		res.setStatus(404);
		return "base";
	}
}


/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.IOException;
import java.util.Collections;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class ErrorController {

	private final ScooldUtils utils;

	@Inject
	public ErrorController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping("/error/{code}")
	public String get(@PathVariable String code, HttpServletRequest req, HttpServletResponse res, Model model) throws IOException {
		model.addAttribute("path", "error.vm");
		model.addAttribute("title", utils.getLang(req).get("error.title"));
		model.addAttribute("status", req.getAttribute("jakarta.servlet.error.status_code"));
		model.addAttribute("reason", req.getAttribute("jakarta.servlet.error.message"));
		model.addAttribute("code", code);

		if (StringUtils.startsWith((CharSequence) req.getAttribute("jakarta.servlet.forward.request_uri"), "/api/")) {
			res.setContentType(MediaType.APPLICATION_JSON_VALUE);
			ParaObjectUtils.getJsonWriterNoIdent().writeValue(res.getOutputStream(),
					Collections.singletonMap("error", code + " - " + req.getAttribute("jakarta.servlet.error.message")));
		}
		return "base";
	}
}


/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Votable;
import com.erudika.para.core.Vote;
import com.erudika.scoold.ScooldConfig;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.CRITIC;
import static com.erudika.scoold.core.Profile.Badge.GOODANSWER;
import static com.erudika.scoold.core.Profile.Badge.GOODQUESTION;
import static com.erudika.scoold.core.Profile.Badge.SUPPORTER;
import static com.erudika.scoold.core.Profile.Badge.VOTER;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class VoteController {

	private static final Logger logger = LoggerFactory.getLogger(VoteController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;
	private final Integer expiresAfterSec;
	private final Integer lockedAfterSec;

	@Inject
	public VoteController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		expiresAfterSec = CONF.voteExpiresAfterSec();
		lockedAfterSec = CONF.voteLockedAfterSec();
	}

	@ResponseBody
	@PostMapping("/voteup/{type}/{id}")
	public Boolean voteup(@PathVariable String type, @PathVariable String id, HttpServletRequest req) {
		ParaObject votable = StringUtils.isBlank(type) ? pc.read(id) : pc.read(type, id);
		return processVoteRequest(true, votable, req);
	}

	@ResponseBody
	@PostMapping("/votedown/{type}/{id}")
	public Boolean votedown(@PathVariable String type, @PathVariable String id, HttpServletRequest req) {
		if (!CONF.downvotesEnabled()) {
			return false;
		}
		ParaObject votable = StringUtils.isBlank(type) ? pc.read(id) : pc.read(type, id);
		return processVoteRequest(false, votable, req);
	}

	boolean processVoteRequest(boolean isUpvote, ParaObject votable, HttpServletRequest req) {
		Profile author = null;
		Profile authUser = utils.getAuthUser(req);
		boolean result = false;
		boolean update = false;
		if (votable == null || authUser == null) {
			return false;
		}

		try {
			List<ParaObject> voteObjects = pc.readAll(Arrays.asList(votable.getCreatorid(),
					new Vote(authUser.getId(), votable.getId(), Votable.VoteValue.UP).getId(),
					new Vote(authUser.getId(), votable.getId(), Votable.VoteValue.DOWN).getId()));

			author = (Profile) voteObjects.stream().filter((p) -> p instanceof Profile).findFirst().orElse(null);
			Integer votes = votable.getVotes() != null ? votable.getVotes() : 0;
			boolean upvoteExists = voteObjects.stream().anyMatch((v) -> v instanceof Vote && ((Vote) v).isUpvote());
			boolean downvoteExists = voteObjects.stream().anyMatch((v) -> v instanceof Vote && ((Vote) v).isDownvote());
			boolean isVoteCorrection = (isUpvote && downvoteExists) || (!isUpvote && upvoteExists);

			if (isUpvote && voteUp(votable, authUser.getId())) {
				votes++;
				result = true;
				update = updateReputationOnUpvote(votable, votes, authUser, author, isVoteCorrection);
			} else if (!isUpvote && voteDown(votable, authUser.getId())) {
				votes--;
				result = true;
				hideCommentAndReport(votable, votes, votable.getId(), req);
				update = updateReputationOnDownvote(votable, votes, authUser, author, isVoteCorrection);
			}
		} catch (Exception ex) {
			logger.error(null, ex);
			result = false;
		}
		utils.addBadgeOnce(authUser, SUPPORTER, authUser.getUpvotes() >= CONF.supporterIfHasRep());
		utils.addBadgeOnce(authUser, CRITIC, authUser.getDownvotes() >= CONF.criticIfHasRep());
		utils.addBadgeOnce(authUser, VOTER, authUser.getTotalVotes() >= CONF.voterIfHasRep());

		if (update) {
			pc.updateAll(Arrays.asList(author, authUser));
		}
		return result;
	}

	private boolean updateReputationOnUpvote(ParaObject votable, Integer votes,
			Profile authUser, Profile author, boolean isVoteCorrection) {
		if (author != null) {
			if (isVoteCorrection) {
				author.addRep(CONF.postVotedownPenaltyAuthor()); // revert penalty to author
				authUser.addRep(CONF.postVotedownPenaltyVoter()); // revert penalty to voter
				authUser.decrementDownvotes();
			} else {
				author.addRep(addReward(votable, author, votes));
				authUser.incrementUpvotes();
			}
			return true;
		}
		return false;
	}

	private boolean updateReputationOnDownvote(ParaObject votable, Integer votes,
			Profile authUser, Profile author, boolean isVoteCorrection) {
		if (author != null) {
			if (isVoteCorrection) {
				author.removeRep(addReward(votable, author, votes));
				authUser.decrementUpvotes();
			} else {
				author.removeRep(CONF.postVotedownPenaltyAuthor()); // small penalty to author
				authUser.removeRep(CONF.postVotedownPenaltyVoter()); // small penalty to voter
				authUser.incrementDownvotes();
			}
			return true;
		}
		return false;
	}

	private int addReward(Votable votable, Profile author, int votes) {
		int reward;
		if (votable instanceof Post) {
			Post p = (Post) votable;
			if (p.isReply()) {
				utils.addBadge(author, GOODANSWER, votes >= CONF.goodAnswerIfHasRep(), false);
				reward = CONF.answerVoteupRewardAuthor();
			} else if (p.isQuestion()) {
				utils.addBadge(author, GOODQUESTION, votes >= CONF.goodQuestionIfHasRep(), false);
				reward = CONF.questionVoteupRewardAuthor();
			} else {
				reward = CONF.voteupRewardAuthor();
			}
		} else {
			reward = CONF.voteupRewardAuthor();
		}
		return reward;
	}

	private void hideCommentAndReport(Votable votable, int votes, String id, HttpServletRequest req) {
		if (votable instanceof Comment && votes <= -5) {
			//treat comment as offensive or spam - hide
			((Comment) votable).setHidden(true);
		} else if (votable instanceof Post && votes <= -5) {
			Post p = (Post) votable;
			//mark post for closing
			Report rep = new Report();
			rep.setParentid(id);
			rep.setLink(p.getPostLink(false, false));
			rep.setDescription(utils.getLang(req).get("posts.forclosing"));
			rep.setSubType(Report.ReportType.OTHER);
			rep.setAuthorName("System");
			rep.addProperty(utils.getLang(req).get("spaces.title"), utils.getSpaceName(p.getSpace()));
			rep.create();
		}
	}

	private boolean voteUp(ParaObject votable, String userid) {
		return pc.voteUp(votable, userid, expiresAfterSec, lockedAfterSec);
	}

	private boolean voteDown(ParaObject votable, String userid) {
		return pc.voteDown(votable, userid, expiresAfterSec, lockedAfterSec);
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Address;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.QUESTIONSLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class QuestionsController {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	private QuestionController questionController;

	@Inject
	public QuestionsController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping({"/", "/questions"})
	public String get(@RequestParam(required = false) String sortby, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK;
		}
		getQuestions(sortby, null, req, model);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title"));
		model.addAttribute("questionsSelected", "navbtn-hover");
		model.addAttribute("questionsTypeFilter", HttpUtils.getCookieValue(req, "questions-type-filter"));
		return "base";
	}

	@GetMapping({"/questions/{id}", "/questions/{id}/{title}", "/questions/{id}/{title}/*"})
	public String getAlias(@PathVariable String id, @PathVariable(required = false) String title,
			@RequestParam(required = false) String sortby, HttpServletRequest req, HttpServletResponse res, Model model) {
		return questionController.get(id, title, sortby, req, res, model);
	}

	@GetMapping("/questions/tag/{tag}")
	public String getTagged(@PathVariable String tag, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + req.getRequestURI();
		}
		Pager itemcount = utils.getPager("page", req);
		List<Question> questionslist = Collections.emptyList();
		String type = Utils.type(Question.class);
		String qf = utils.getSpaceFilteredQuery(req);
		if (!qf.isEmpty()) {
			if (qf.equals("*")) {
				questionslist = pc.findTagged(type, new String[]{tag}, itemcount);
			} else {
				questionslist = pc.findQuery(type, qf + " AND " + Config._TAGS + ":" + tag + "*", itemcount);
			}
		}
		int c = (int) itemcount.getCount();
		Tag t = pc.read(new Tag(tag).getId());
		if (t != null && t.getCount() != c && utils.isMod(utils.getAuthUser(req))) {
			t.setCount(c);
			pc.update(t);
		}
		utils.getProfiles(questionslist);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("posts.tagged") + " - " + tag);
		model.addAttribute("questionsSelected", "navbtn-hover");
		model.addAttribute("tag", tag);
		model.addAttribute("tagDescription", t != null ? t.getDescription() : "");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("questionslist", questionslist);
		return "base";
	}

	@GetMapping("/questions/similar/{like}")
	public void getSimilarAjax(@PathVariable String like, HttpServletRequest req, HttpServletResponse res) throws IOException {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			res.setStatus(401);
			return;
		}
		Pager pager = new Pager(1, "votes", true, CONF.maxSimilarPosts());
		Profile authUser = utils.getAuthUser(req);
		StringBuilder sb = new StringBuilder();
		Question q = new Question();
		q.setTitle(like);
		q.setBody("");
		q.setTags(Arrays.asList(""));
		for (Post similarPost : utils.getSimilarPosts(q, pager)) {
			if (utils.isMod(authUser) || utils.canAccessSpace(authUser, similarPost.getSpace())) {
				boolean hasAnswer = !StringUtils.isBlank(similarPost.getAnswerid());
				sb.append("<span class=\"lightborder phm").append(hasAnswer ? " light-green white-text" : "").append("\">");
				sb.append(similarPost.getVotes());
				sb.append("</span> <a href=\"").append(similarPost.getPostLink(false, false)).append("\">");
				sb.append(HtmlUtils.htmlEscape(similarPost.getTitle())).append("</a><br>");
			}
		}
		res.setCharacterEncoding("UTF-8");
		res.getWriter().print(sb.toString());
		res.setStatus(200);
	}

	@GetMapping({"/questions/favtags", "/questions/local"})
	public String getSorted(@RequestParam(required = false) String sortby, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + req.getRequestURI();
		}
		getQuestions(sortby, req.getServletPath().endsWith("/favtags") ? "favtags" : "local", req, model);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title"));
		model.addAttribute("questionsSelected", "navbtn-hover");
		return "base";
	}

	@PostMapping("/questions/apply-filter")
	public String applyFilter(@RequestParam(required = false) String sortby,
			@RequestParam(required = false) String typeFilter, @RequestParam(required = false) String tab,
			@RequestParam(required = false, defaultValue = "false") String compactViewEnabled,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (req.getParameter("clear") != null) {
			HttpUtils.removeStateParam("questions-filter", req, res);
			HttpUtils.removeStateParam("questions-type-filter", req, res);
			HttpUtils.removeStateParam("questions-view-compact", req, res);
		} else {
			Pager p = utils.pagerFromParams(req);
			if (!StringUtils.isBlank(req.getParameter(Config._TAGS))) {
				boolean matchAll = "true".equals(req.getParameter("matchAllTags"));
				p.setName("with_tags:" + (matchAll ? "+" : "") + req.getParameter(Config._TAGS));
			}
			if (!StringUtils.isBlank(typeFilter)) {
				HttpUtils.setRawCookie("questions-type-filter", typeFilter,
					req, res, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
			} else {
				HttpUtils.removeStateParam("questions-type-filter", req, res);
			}
			savePagerToCookie(req, res, p);
			HttpUtils.setRawCookie("questions-view-compact", compactViewEnabled,
					req, res, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
		}
		return "redirect:" + QUESTIONSLINK + (StringUtils.isBlank(sortby) ? "" : "?sortby="
				+ Optional.ofNullable(StringUtils.trimToNull(sortby)).orElse(tab));
	}

	@GetMapping("/questions/ask")
	public String ask(HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK + "/ask";
		}
		model.addAttribute("path", "questions.vm");
		model.addAttribute("askSelected", "navbtn-hover");
		model.addAttribute("defaultTag", CONF.defaultQuestionTag());
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		model.addAttribute("includeEmojiPicker", true);
		model.addAttribute("title", utils.getLang(req).get("posts.ask"));

		Question draft = utils.populate(req, new Question(), "title", "body", "tags|,", "location", "space");
		String sid = Post.DEFAULT_SPACE;
		if (utils.isAuthenticated(req)) {
			sid = utils.getSpaceId(utils.getSpaceIdFromCookie(utils.getAuthUser(req), req));
			sid = StringUtils.replace(sid, "*", "default");
		}
		Sysprop spaceObj =  pc.read(sid);
		if (spaceObj != null) {
			String title = (String) spaceObj.getProperty("titleTemplate");
			String body = (String) spaceObj.getProperty("bodyTemplate");
			String tags = (String) spaceObj.getProperty("tagsTemplate");
			if (!StringUtils.isBlank(title)) {
				draft.setTitle(title);
			}
			if (!StringUtils.isBlank(body)) {
				draft.setBody(body);
			}
			if (!StringUtils.isBlank(tags)) {
				draft.setTags(Arrays.asList(tags.split(",")));
			}
		}
		model.addAttribute("draftQuestion", draft);
		return "base";
	}

	@PostMapping("/questions/ask")
	public String post(@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String address, String space, String postId,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			String currentSpace = utils.getValidSpaceIdExcludingAll(authUser, space, req);
			boolean needsApproval = utils.postsNeedApproval(req) && utils.userNeedsApproval(authUser);
			Question q = utils.populate(req, needsApproval ? new UnapprovedQuestion() : new Question(),
					"title", "body", "tags|,", "location");
			q.setCreatorid(authUser.getId());
			q.setAuthor(authUser);
			q.setSpace(currentSpace);
			if (StringUtils.isBlank(q.getTagsString())) {
				q.setTags(Arrays.asList(CONF.defaultQuestionTag().isBlank() ? "" : CONF.defaultQuestionTag()));
			}
			Map<String, String> error = utils.validateQuestionTags(q, utils.validate(q), req);
			q = handleSpam(q, authUser, error, req);
			if (error.isEmpty()) {
				String qid = StringUtils.isBlank(postId) ? Utils.getNewId() : postId;
				q.setId(qid);
				q.setLocation(location);
				q.create();
				utils.sendNewPostNotifications(q, needsApproval, req);
				if (!StringUtils.isBlank(latlng)) {
					Address addr = new Address(qid + Para.getConfig().separator() + Utils.type(Address.class));
					addr.setAddress(address);
					addr.setCountry(location);
					addr.setLatlng(latlng);
					addr.setParentid(qid);
					addr.setCreatorid(authUser.getId());
					pc.create(addr);
				}
				authUser.setLastseen(System.currentTimeMillis());
				model.addAttribute("newpost", getNewQuestionPayload(q));
			} else {
				model.addAttribute("error", error);
				model.addAttribute("draftQuestion", q);
				model.addAttribute("defaultTag", "");
				model.addAttribute("path", "questions.vm");
				model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
				model.addAttribute("askSelected", "navbtn-hover");
				res.setStatus(400);
				return "base";
			}
			if (utils.isAjaxRequest(req)) {
				res.setStatus(200);
				res.setContentType("application/json");
				try {
					res.getWriter().println("{\"url\":\"" + q.getPostLink(false, false) + "\"}");
				} catch (IOException ex) { }
				return "blank";
			} else {
				return "redirect:" + q.getPostLinkForRedirect();
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(400);
			return "blank";
		} else {
			return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK + "/ask";
		}
	}

	@GetMapping({"/questions/space/{space}", "/questions/space"})
	public String setSpace(@PathVariable(required = false) String space,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if ("all".equals(space) || utils.isAllSpaces(space)) {
			space = Post.ALL_MY_SPACES + ":" + utils.getLang(req).get("allspaces");
		} else {
			Sysprop spaceObj = pc.read(utils.getSpaceId(space));
			if (!StringUtils.isBlank(space) && spaceObj == null) {
				Profile authUser = utils.getAuthUser(req);
				if (authUser != null && utils.canAccessSpace(authUser, space)) {
					authUser.removeSpace(space);
					authUser.update();
				}
			}
			if (spaceObj != null && !utils.isDefaultSpace(spaceObj.getId())) {
				space = spaceObj.getId().concat(Para.getConfig().separator()).concat(spaceObj.getName());
			} else {
				space = Post.DEFAULT_SPACE;
			}
		}
		utils.storeSpaceIdInCookie(space, req, res);
		String backTo = HttpUtils.getBackToUrl(req, true);
		if (!utils.isAuthenticated(req) && !(utils.isDefaultSpace(space) || utils.isAllSpaces(space))) {
			return "redirect:" + SIGNINLINK + "?returnto=" + req.getRequestURI();
		}
		if (StringUtils.isBlank(backTo) || backTo.equalsIgnoreCase(req.getRequestURI())) {
			return get(req.getParameter("sortby"), req, model);
		} else {
			return "redirect:" + backTo;
		}
	}

	@PostMapping("/questions/save-template")
	public String saveTemplate(@RequestParam(required = false) String title, @RequestParam(required = false) String body,
			@RequestParam(required = false) String tags, HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			String space = utils.getSpaceIdFromCookie(authUser, req);
			Sysprop spaceObj = pc.read(utils.getSpaceId(space));
			if (spaceObj == null) {
				spaceObj = utils.buildSpaceObject("default");
			}
			spaceObj.addProperty("titleTemplate", title);
			spaceObj.addProperty("bodyTemplate", body);
			spaceObj.addProperty("tagsTemplate", tags);
			pc.update(spaceObj);
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "blank";
		} else {
			return "redirect:" + QUESTIONSLINK + "/ask";
		}
	}

	public List<Question> getQuestions(String sortby, String filter, HttpServletRequest req, Model model) {
		Pager itemcount = getPagerFromCookie(req, utils.getPager("page", req));
		List<Question> questionslist = Collections.emptyList();
		String type = Utils.type(Question.class);
		Profile authUser = utils.getAuthUser(req);
		String currentSpace = utils.getSpaceIdFromCookie(authUser, req);
		String query = getQuestionsQuery(req, authUser, sortby, currentSpace, itemcount);

		if (!StringUtils.isBlank(filter) && authUser != null) {
			if ("favtags".equals(filter)) {
				if (!authUser.hasFavtags() && req.getParameterValues("favtags") != null) {
					authUser.setFavtags(Arrays.asList(req.getParameterValues("favtags"))); // API override
				}
				if (isSpaceFilteredRequest(authUser, currentSpace) && authUser.hasFavtags()) {
					questionslist = pc.findQuery(type, getSpaceFilteredFavtagsQuery(currentSpace, authUser), itemcount);
				} else {
					questionslist = pc.findTermInList(type, Config._TAGS, authUser.getFavtags(), itemcount);
				}
			} else if ("local".equals(filter)) {
				String latlng = Optional.ofNullable(authUser.getLatlng()).orElse(req.getParameter("latlng"));
				String[] ll =  latlng == null ? new String[0] : latlng.split(",");
				if (ll.length == 2) {
					double lat = NumberUtils.toDouble(ll[0]);
					double lng = NumberUtils.toDouble(ll[1]);
					questionslist = pc.findNearby(type, query, 25, lat, lng, itemcount);
				}
			}
			model.addAttribute("localFilterOn", "local".equals(filter));
			model.addAttribute("tagFilterOn", "favtags".equals(filter));
			model.addAttribute("filter", "/" + Utils.stripAndTrim(filter));
		} else {
			questionslist = pc.findQuery(type, query, itemcount);
		}

		if (utils.postsNeedApproval(req) && utils.isMod(authUser)) {
			Pager p = new Pager(itemcount.getPage(), itemcount.getLimit());
			List<UnapprovedQuestion> uquestionslist = pc.findQuery(Utils.type(UnapprovedQuestion.class), query, p);
			List<Question> qlist = new LinkedList<>(uquestionslist);
			itemcount.setCount(itemcount.getCount() + p.getCount());
			qlist.addAll(questionslist);
			questionslist = qlist;
		}

		utils.getProfiles(questionslist);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("questionslist", questionslist);
		return questionslist;
	}

	private String getSpaceFilteredFavtagsQuery(String currentSpace, Profile authUser) {
		StringBuilder sb = new StringBuilder(utils.getSpaceFilter(authUser, currentSpace));
		if (authUser.hasFavtags()) {
			// should we specify the tags property here? like: tags:(tag1 OR tag2)
			sb.append(" AND (").append(authUser.getFavtags().stream().collect(Collectors.joining(" OR "))).append(")");
		}
		return sb.toString();
	}

	private String getQuestionsQuery(HttpServletRequest req, Profile authUser, String sortby, String currentSpace, Pager p) {
		boolean spaceFiltered = isSpaceFilteredRequest(authUser, currentSpace);
		String query = utils.getSpaceFilteredQuery(req, spaceFiltered, null, utils.getSpaceFilter(authUser, currentSpace));
		String spaceFilter = utils.getSpaceFilter(authUser, currentSpace);
		spaceFilter = StringUtils.isBlank(spaceFilter) || spaceFilter.startsWith("*") ? "" : spaceFilter + " AND ";
		if ("activity".equals(sortby)) {
			p.setSortby("properties.lastactivity");
		} else if ("votes".equals(sortby)) {
			p.setSortby("votes");
		} else if ("answered".equals(sortby)) {
			p.setSortby("timestamp");
			String q = "properties.answerid:[* TO *]";
			query = utils.getSpaceFilteredQuery(req, spaceFiltered, spaceFilter + q, q);
		} else if ("unanswered".equals(sortby)) {
			p.setSortby("timestamp");
			if ("default_pager".equals(p.getName()) && p.isDesc()) {
				p.setDesc(false);
			}
			String q = "properties.answercount:0";
			query = utils.getSpaceFilteredQuery(req, spaceFiltered, spaceFilter + q, q);
		}
		String tags = StringUtils.trimToEmpty(StringUtils.removeStart(p.getName(), "with_tags:"));
		if (StringUtils.startsWith(p.getName(), "with_tags:") && !StringUtils.isBlank(tags)) {
			String logicalOperator = tags.startsWith("+") ? " AND " : " OR ";
			tags = StringUtils.remove(tags, "+");
			StringBuilder sb = new StringBuilder("*".equals(query) ? "" : query.concat(" AND "));
			// should we specify the tags property here? like: tags:(tag1 OR tag2)
			sb.append("tags").append(":(").append(tags.replaceAll(",", logicalOperator)).append(")");
			query = sb.toString();
		}
		return getQueryWithPossibleExtension(query, req);
	}

	private String getQueryWithPossibleExtension(String query, HttpServletRequest req) {
		String queryExt = req.getParameter("q");
		if (StringUtils.isBlank(queryExt) || queryExt.startsWith("*")) {
			queryExt = StringUtils.trimToEmpty(HttpUtils.getCookieValue(req, "questions-type-filter"));
		}
		queryExt = Utils.urlDecode(queryExt);
		if (!queryExt.isBlank()) {
			return query.equals("*") ? queryExt : query + " AND (" + queryExt + ")";
		}
		return query;
	}

	private boolean isSpaceFilteredRequest(Profile authUser, String space) {
		return utils.canAccessSpace(authUser, space);
	}

	private Pager getPagerFromCookie(HttpServletRequest req, Pager defaultPager) {
		try {
			defaultPager.setName("default_pager");
			String cookie = HttpUtils.getCookieValue(req, "questions-filter");
			if (StringUtils.isBlank(cookie)) {
				return defaultPager;
			}
			Pager pager = ParaObjectUtils.getJsonReader(Pager.class).readValue(Utils.base64dec(cookie));
			pager.setPage(defaultPager.getPage());
			pager.setLastKey(null);
			pager.setCount(0);
			return pager;
		} catch (JsonProcessingException ex) {
			return Optional.ofNullable(defaultPager).orElse(new Pager(CONF.maxItemsPerPage()) {
				public String getName() {
					return "default_pager";
				}
			});
		}
	}

	private void savePagerToCookie(HttpServletRequest req, HttpServletResponse res, Pager p) {
		try {
			HttpUtils.setRawCookie("questions-filter", Utils.base64enc(ParaObjectUtils.getJsonWriterNoIdent().
					writeValueAsBytes(p)), req, res, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
		} catch (JsonProcessingException ex) { }
	}

	private Map<String, Object> getNewQuestionPayload(Question q) {
		Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(q, false));
		payload.put("author", q == null ? null : q.getAuthor());
		utils.triggerHookEvent("question.create", payload);
		return payload;
	}

	private Question handleSpam(Question q, Profile authUser, Map<String, String> error, HttpServletRequest req) {
		boolean isSpam = utils.isSpam(q, authUser, req);
		if (isSpam && CONF.automaticSpamProtectionEnabled()) {
			error.put("body", "spam");
		} else if (isSpam && !CONF.automaticSpamProtectionEnabled()) {
			UnapprovedQuestion spamq = new UnapprovedQuestion();
			spamq.setTitle(q.getTitle());
			spamq.setBody(q.getBody());
			spamq.setTags(q.getTags());
			spamq.setLocation(q.getLocation());
			spamq.setCreatorid(q.getCreatorid());
			spamq.setAuthor(authUser);
			spamq.setSpace(q.getSpace());
			spamq.setSpam(true);
			return spamq;
		}
		return q;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.COMMENTATOR;
import static com.erudika.scoold.core.Profile.Badge.DISCIPLINED;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/comment")
public class CommentController {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public CommentController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping("/{id}")
	public String get(@PathVariable String id, HttpServletRequest req, Model model) {
		Comment showComment = pc.read(id);
		if (showComment == null || !ParaObjectUtils.typesMatch(showComment)) {
			return "redirect:" + HOMEPAGE;
		}
		model.addAttribute("path", "comment.vm");
		model.addAttribute("title", utils.getLang(req).get("comment.title"));
		model.addAttribute("showComment", showComment);
		return "base";
	}

	@GetMapping(params = {Config._PARENTID, "getcomments"})
	public String getAjax(@RequestParam String parentid, @RequestParam Boolean getcomments,
			@RequestParam(required = false, defaultValue = "1") Integer page, HttpServletRequest req, Model model) {
		Post parent = pc.read(parentid);
		if (parent != null) {
			parent.getItemcount().setPage(page);
			List<Comment> commentslist = pc.getChildren(parent, Utils.type(Comment.class), parent.getItemcount());
			parent.setComments(commentslist);
			model.addAttribute("showpost", parent);
			model.addAttribute("itemcount", parent.getItemcount());
		}
		return "comment";
	}

	@PostMapping("/{id}/delete")
	public void deleteAjax(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Comment comment = pc.read(id);
			Profile authUser = utils.getAuthUser(req);
			boolean isMod = utils.isMod(authUser);
			if (comment != null && (comment.getCreatorid().equals(authUser.getId()) || isMod)) {
				// check parent and correct (for multi-parent-object pages)
				comment.delete();
				if (!isMod) {
					utils.addBadgeAndUpdate(authUser, DISCIPLINED, true);
				}
			}
		}
		res.setStatus(200);
	}

	@PostMapping
	public String createAjax(@RequestParam String comment, @RequestParam String parentid,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.canComment(authUser, req) && !StringUtils.isBlank(comment) && !StringUtils.isBlank(parentid)) {
			Comment showComment = utils.populate(req, new Comment(), "comment");
			showComment.setCreatorid(authUser.getId());
			Map<String, String> error = utils.validate(showComment);
			handleSpam(showComment, authUser, error, req);
			if (error.isEmpty()) {
				showComment.setComment(comment);
				showComment.setParentid(parentid);
				showComment.setAuthorName(authUser.getName());

				if (showComment.create() != null) {
					long commentCount = authUser.getComments();
					utils.addBadgeOnce(authUser, COMMENTATOR, commentCount >= CONF.commentatorIfHasRep());
					authUser.setComments(commentCount + 1);
					authUser.update();
					model.addAttribute("showComment", showComment);
					// send email to the author of parent post
					Post parentPost = pc.read(parentid);
					if (parentPost != null && parentPost.addCommentId(showComment.getId())) {
						pc.update(parentPost); // update without adding revisions
					}
					utils.sendCommentNotifications(parentPost, showComment, authUser, req);
				}
			}
		}
		return "comment";
	}

	private void handleSpam(Comment c, Profile authUser, Map<String, String> error, HttpServletRequest req) {
		boolean isSpam = utils.isSpam(c, authUser, req);
		if (isSpam && CONF.automaticSpamProtectionEnabled()) {
			error.put("comment", "spam");
		} else if (isSpam && !CONF.automaticSpamProtectionEnabled()) {
			Report rep = new Report();
			rep.setContent(Utils.abbreviate(Utils.markdownToHtml(c.getComment()), 2000));
			rep.setParentid(c.getId());
			rep.setCreatorid(authUser.getId());
			rep.setDescription("SPAM detected");
			rep.setSubType(Report.ReportType.SPAM);
			rep.setLink(CONF.serverUrl() + "/comment/" + c.getId());
			rep.setAuthorName(authUser.getName());
			rep.create();
		}
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.annotations.Email;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import static com.erudika.scoold.utils.HttpUtils.getBackToUrl;
import static com.erudika.scoold.utils.HttpUtils.setAuthCookie;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SigninController {

	private static final Logger logger = LoggerFactory.getLogger(SigninController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public SigninController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping("/signin")
	public String get(@RequestParam(name = "returnto", required = false, defaultValue = HOMEPAGE) String returnto,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (utils.isAuthenticated(req)) {
			return "redirect:" + (StringUtils.startsWithIgnoreCase(returnto, SIGNINLINK) ? HOMEPAGE : getBackToUrl(req));
		}
		if (!HOMEPAGE.equals(returnto) && !StringUtils.startsWith(returnto, SIGNINLINK)) {
			HttpUtils.setStateParam("returnto", Utils.urlEncode(getBackToUrl(req)), req, res);
		} else {
			HttpUtils.removeStateParam("returnto", req, res);
		}
		if (CONF.redirectSigninToIdp() && !"5".equals(req.getParameter("code"))) {
			return "redirect:" + utils.getFirstConfiguredLoginURL();
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("signin.title"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("fbLoginEnabled", !CONF.facebookAppId().isEmpty());
		model.addAttribute("gpLoginEnabled", !CONF.googleAppId().isEmpty());
		model.addAttribute("ghLoginEnabled", !CONF.githubAppId().isEmpty());
		model.addAttribute("inLoginEnabled", !CONF.linkedinAppId().isEmpty());
		model.addAttribute("twLoginEnabled", !CONF.twitterAppId().isEmpty());
		model.addAttribute("msLoginEnabled", utils.isMicrosoftAuthEnabled());
		model.addAttribute("slLoginEnabled", utils.isSlackAuthEnabled());
		model.addAttribute("azLoginEnabled", !CONF.amazonAppId().isEmpty());
		model.addAttribute("oa2LoginEnabled", !CONF.oauthAppId("").isEmpty());
		model.addAttribute("oa2secondLoginEnabled", !CONF.oauthAppId("second").isEmpty());
		model.addAttribute("oa2thirdLoginEnabled", !CONF.oauthAppId("third").isEmpty());
		model.addAttribute("ldapLoginEnabled", !CONF.ldapServerUrl().isEmpty());
		model.addAttribute("passwordLoginEnabled", CONF.passwordAuthEnabled());
		model.addAttribute("oa2LoginProvider", CONF.oauthProvider(""));
		model.addAttribute("oa2secondLoginProvider", CONF.oauthProvider("second"));
		model.addAttribute("oa2thirdLoginProvider", CONF.oauthProvider("third"));
		model.addAttribute("ldapLoginProvider", CONF.ldapProvider());
		return "base";
	}

	@PostMapping(path = "/signin", params = {"access_token", "provider"})
	public String signinPost(@RequestParam("access_token") String accessToken, @RequestParam("provider") String provider,
			HttpServletRequest req, HttpServletResponse res) {
		return getAuth(provider, accessToken, req, res);
	}

	@GetMapping("/signin/success")
	public String signinSuccess(@RequestParam String jwt, HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!StringUtils.isBlank(jwt)) {
			if (!utils.isAuthenticated(req)) {
				return loginWithIdToken(jwt, req, res);
			} else {
				return "redirect:" + getBackToUrl(req);
			}
		} else {
			return "redirect:" + SIGNINLINK + "?code=3&error=true";
		}
	}

	@GetMapping(path = "/signin/register")
	public String register(@RequestParam(name = "verify", required = false, defaultValue = "false") Boolean verify,
			@RequestParam(name = "resend", required = false, defaultValue = "false") Boolean resend,
			@RequestParam(name = "id", required = false) String id,
			@RequestParam(name = "token", required = false) String token,
			HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req) || !CONF.passwordAuthEnabled()) {
			return "redirect:" + SIGNINLINK;
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("signup.title"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("emailPattern", Email.EMAIL_PATTERN);
		model.addAttribute("register", true);
		model.addAttribute("verify", verify);
		model.addAttribute("resend", resend);
		model.addAttribute("bademail", req.getParameter("email"));
		model.addAttribute("nosmtp", StringUtils.isBlank(CONF.mailHost()));
		model.addAttribute("captchakey", CONF.captchaSiteKey());
		if (id != null && token != null) {
			User u = (User) pc.read(id);
			boolean verified = activateWithEmailToken(u, token);
			if (verified) {
				model.addAttribute("verified", verified);
				model.addAttribute("verifiedEmail", u.getEmail());
			} else {
				return "redirect:" + SIGNINLINK;
			}
		}
		return "base";
	}

	@PostMapping("/signin/register")
	public String signup(@RequestParam String name, @RequestParam String email, @RequestParam String passw,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!CONF.passwordAuthEnabled()) {
			return "redirect:" + SIGNINLINK;
		}
		boolean approvedDomain = utils.isEmailDomainApproved(email);
		if (!utils.isAuthenticated(req) && HttpUtils.isValidCaptcha(req.getParameter("g-recaptcha-response"))) {
			boolean goodPass = utils.isPasswordStrongEnough(passw);
			if (!isEmailRegistered(email) && approvedDomain && isSubmittedByHuman(req) && goodPass) {
				User u = pc.signIn("password", email + ":" + name + ":" + passw, false);
				if (u != null && u.getActive()) {
					setAuthCookie(u, req, res);
					triggerLoginEvent(u, req);
					return "redirect:" + getBackToUrl(req);
				} else {
					verifyEmailIfNecessary(name, email, req);
				}
			} else {
				Map<String, String> errors = new HashMap<String, String>();
				model.addAttribute("path", "signin.vm");
				model.addAttribute("title", utils.getLang(req).get("signup.title"));
				model.addAttribute("signinSelected", "navbtn-hover");
				model.addAttribute("register", true);
				model.addAttribute("name", name);
				model.addAttribute("bademail", email);
				model.addAttribute("emailPattern", Email.EMAIL_PATTERN);
				if (!goodPass) {
					errors.put("passw", utils.getLang(req).get("msgcode.8"));
				} else {
					errors.put("email", utils.getLang(req).get("msgcode." + (approvedDomain ? "1" : "9")));
				}
				model.addAttribute("error", errors);
				return "base";
			}
		}
		return "redirect:" + SIGNINLINK + (approvedDomain ? "/register?verify=true" : "?code=9&error=true");
	}

	@PostMapping("/signin/register/resend")
	public String resend(@RequestParam String email, HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!utils.isAuthenticated(req) && HttpUtils.isValidCaptcha(req.getParameter("g-recaptcha-response"))) {
			Sysprop ident = pc.read(email);
			// confirmation emails can be resent once every 6h
			if (ident != null && !StringUtils.isBlank((String) ident.getProperty(Config._EMAIL_TOKEN))) {
				if (!ident.hasProperty("confirmationTimestamp") || Utils.timestamp() >
					((long) ident.getProperty("confirmationTimestamp") + TimeUnit.HOURS.toMillis(6))) {
					User u = pc.read(Utils.type(User.class), ident.getCreatorid());
					if (u != null && !u.getActive()) {
						utils.sendVerificationEmail(ident, "", "", req);
					}
				} else {
					logger.warn("Failed to send email confirmation to '{}' - this can only be done once every 6h.", email);
				}
			} else {
				logger.warn("Failed to send email confirmation to '{}' - user has not signed in with that email yet.", email);
			}
		}
		return "redirect:" + SIGNINLINK + "/register?verify=true";
	}

	@GetMapping(path = "/signin/iforgot")
	public String iforgot(@RequestParam(name = "verify", required = false, defaultValue = "false") Boolean verify,
			@RequestParam(name = "email", required = false) String email,
			@RequestParam(name = "token", required = false) String token,
			HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req) || !CONF.passwordAuthEnabled()) {
			return "redirect:" + SIGNINLINK;
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("iforgot.title"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("iforgot", true);
		model.addAttribute("verify", verify);
		model.addAttribute("nosmtp", StringUtils.isBlank(CONF.mailHost()));
		model.addAttribute("captchakey", CONF.captchaSiteKey());
		if (email != null && token != null) {
			model.addAttribute("email", email);
			model.addAttribute("token", token);
		}
		return "base";
	}

	@PostMapping("/signin/iforgot")
	public String changePass(@RequestParam String email,
			@RequestParam(required = false) String newpassword,
			@RequestParam(required = false) String token,
			HttpServletRequest req, Model model) {
		if (!CONF.passwordAuthEnabled()) {
			return "redirect:" + SIGNINLINK;
		}
		boolean approvedDomain = utils.isEmailDomainApproved(email);
		boolean validCaptcha = HttpUtils.isValidCaptcha(req.getParameter("g-recaptcha-response"));
		if (!utils.isAuthenticated(req) && approvedDomain && validCaptcha) {
			if (StringUtils.isBlank(token)) {
				generatePasswordResetToken(email, req);
				return "redirect:" + SIGNINLINK + "/iforgot?verify=true";
			} else {
				boolean error = !resetPassword(email, newpassword, token);
				model.addAttribute("path", "signin.vm");
				model.addAttribute("title", utils.getLang(req).get("iforgot.title"));
				model.addAttribute("signinSelected", "navbtn-hover");
				model.addAttribute("iforgot", true);
				model.addAttribute("email", email);
				model.addAttribute("token", token);
				model.addAttribute("verified", !error);
				model.addAttribute("captchakey", CONF.captchaSiteKey());
				if (error) {
					if (!utils.isPasswordStrongEnough(newpassword)) {
						model.addAttribute("error", Collections.singletonMap("newpassword", utils.getLang(req).get("msgcode.8")));
					} else {
						model.addAttribute("error", Collections.singletonMap("email", utils.getLang(req).get("msgcode.7")));
					}
				}
				return "base";
			}
		}
		logger.info("Password reset failed for {} - authenticated={}, approvedDomain={}, validCaptcha={}",
				email, utils.isAuthenticated(req), approvedDomain, validCaptcha);
		return "redirect:" + SIGNINLINK + "/iforgot";
	}

	@GetMapping("/signin/two-factor")
	public String twoFA(HttpServletRequest req, Model model) {
		String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
		if (StringUtils.isBlank(jwt)) {
			return "redirect:" + SIGNINLINK;
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("signin.twofactor"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("twofactor", true);
		return "base";
	}

	@PostMapping("/signin/two-factor")
	public String twoFAVerify(@RequestParam(name = "reset", required = false, defaultValue = "false") Boolean reset,
			@RequestParam String code, HttpServletRequest req, HttpServletResponse res, Model model) {
		String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
		if (StringUtils.isBlank(jwt)) {
			return "redirect:" + SIGNINLINK;
		}
		User u = pc.me(jwt);
		if (u == null) {
			return "redirect:" + SIGNINLINK + "?code=3&error=true";
		}
		if (reset && Utils.bcryptMatches(code, u.getTwoFAbackupKeyHash())) {
			u.setTwoFA(false);
			u.setTwoFAkey("");
			u.setTwoFAbackupKeyHash("");
			pc.update(u);
			return "redirect:" + getBackToUrl(req);
		} else {
			int totpKey = NumberUtils.toInt(code, 0);
			if (utils.isValid2FACode(u.getTwoFAkey(), totpKey, 0)) {
				HttpUtils.set2FACookie(u, utils.getUnverifiedClaimsFromJWT(jwt).getIssueTime(), req, res);
				return "redirect:" + getBackToUrl(req);
			} else {
				model.addAttribute("path", "signin.vm");
				model.addAttribute("title", utils.getLang(req).get("signin.twofactor"));
				model.addAttribute("signinSelected", "navbtn-hover");
				model.addAttribute("twofactor", true);
				model.addAttribute("error", Map.of("code", utils.getLang(req).get("signin.invalidcode")));
				return "base";
			}
		}
	}

	@PostMapping("/signout")
	public String post(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			utils.clearSession(req, res);
			return "redirect:" + CONF.signoutUrl();
		}
		return "redirect:" + HOMEPAGE;
	}

	private String getAuth(String provider, String accessToken, HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isAuthenticated(req)) {
			if (StringUtils.equalsAnyIgnoreCase(accessToken, "password", "ldap")) {
				accessToken = req.getParameter("username") + ":" +
						("password".equals(accessToken) ? ":" : "") +
						req.getParameter("password");
			}
			String email = getEmailFromAccessToken(accessToken);
			if ("password".equals(provider) && !isEmailRegistered(email)) {
				return "redirect:" + SIGNINLINK + "?code=3&error=true";
			}
			User u = pc.signIn(provider, accessToken, false);
			if (u == null && isAccountLocked(email)) {
				return "redirect:" + SIGNINLINK + "?code=6&error=true&email=" + email;
			}
			return onAuthSuccess(u, req, res);
		}
		return "redirect:" + getBackToUrl(req);
	}

	private String loginWithIdToken(String jwt, HttpServletRequest req, HttpServletResponse res) {
		User u = pc.signIn("passwordless", jwt, false);
		return onAuthSuccess(u, req, res);
	}

	private String onAuthSuccess(User u, HttpServletRequest req, HttpServletResponse res) {
		if (u != null && utils.isEmailDomainApproved(u.getEmail())) {
			// the user password in this case is a Bearer token (JWT)
			setAuthCookie(u, req, res);
			triggerLoginEvent(u, req);
			return "redirect:" + getBackToUrl(req);
		} else if (u != null && !utils.isEmailDomainApproved(u.getEmail())) {
			logger.warn("Signin failed for {} because that domain is not in the whitelist.", u.getEmail());
			return "redirect:" + SIGNINLINK + "?code=9&error=true";
		}
		return "redirect:" + SIGNINLINK + "?code=3&error=true";
	}

	private boolean activateWithEmailToken(User u, String token) {
		if (u != null && token != null) {
			Sysprop s = pc.read(u.getIdentifier());
			if (s != null && token.equals(s.getProperty(Config._EMAIL_TOKEN))) {
				s.addProperty(Config._EMAIL_TOKEN, "");
				pc.update(s);
				u.setActive(true);
				pc.update(u);
				return true;
			}
			logger.warn("Failed to verify user with email '{}' - invalid verification token.", u.getEmail());
		}
		return false;
	}

	private String getEmailFromAccessToken(String accessToken) {
		String[] tokenParts = StringUtils.split(accessToken, ":");
		return (tokenParts != null && tokenParts.length > 0) ? StringUtils.toRootLowerCase(tokenParts[0]) : "";
	}

	private boolean isEmailRegistered(String email) {
		if (StringUtils.isBlank(email)) {
			return false;
		}
		Sysprop ident = pc.read(email.toLowerCase());
		return ident != null && ident.hasProperty(Config._PASSWORD);
	}

	private boolean isAccountLocked(String email) {
		if (!StringUtils.isBlank(email)) {
			Sysprop ident = pc.read(email.toLowerCase());
			if (ident != null && !StringUtils.isBlank((String) ident.getProperty(Config._EMAIL_TOKEN))) {
				User u = pc.read(Utils.type(User.class), ident.getCreatorid());
				return u != null && !u.getActive();
			}
		}
		return false;
	}

	private void verifyEmailIfNecessary(String name, String email, HttpServletRequest req) {
		if (!StringUtils.isBlank(email)) {
			email = email.toLowerCase();
			Sysprop ident = pc.read(email);
			if (ident != null && !ident.hasProperty(Config._EMAIL_TOKEN)) {
				User u = new User(ident.getCreatorid());
				u.setActive(false);
				u.setName(name);
				u.setEmail(email);
				u.setIdentifier(email);
				utils.sendWelcomeEmail(u, true, req);
			}
		}
	}

	private boolean isSubmittedByHuman(HttpServletRequest req) {
		long time = NumberUtils.toLong(req.getParameter("timestamp"), 0L);
		return StringUtils.isBlank(req.getParameter("leaveblank")) && (System.currentTimeMillis() - time >= 7000);
	}

	private String generatePasswordResetToken(String email, HttpServletRequest req) {
		if (StringUtils.isBlank(email)) {
			return "";
		}
		email = email.toLowerCase();
		Sysprop s = pc.read(email);
		// pass reset emails can be sent once every 12h
		if (s != null) {
			if (!s.hasProperty("iforgotTimestamp") || Utils.timestamp() >
						(Long.valueOf(s.getProperty("iforgotTimestamp").toString()) + TimeUnit.HOURS.toMillis(12))) {
				String token = Utils.generateSecurityToken(42, true);
				s.addProperty(Config._RESET_TOKEN, token);
				s.addProperty("iforgotTimestamp", Utils.timestamp());
				s.setUpdated(Utils.timestamp());
				if (pc.update(s) != null) {
					utils.sendPasswordResetEmail(email, token, req);
				}
				return token;
			} else {
				logger.warn("Failed to send password reset email to '{}' - this can only be done once every 12h.", email);
			}
		} else {
			logger.warn("Failed to send password reset email to '{}' - user has not signed in with that email and passowrd.", email);
		}
		return "";
	}

	private boolean resetPassword(String email, String newpass, String token) {
		if (StringUtils.isBlank(newpass) || StringUtils.isBlank(token) || !utils.isPasswordStrongEnough(newpass)) {
			return false;
		}
		Sysprop s = pc.read(email);
		if (isValidResetToken(s, Config._RESET_TOKEN, token)) {
			s.addProperty(Config._RESET_TOKEN, ""); // avoid removeProperty method because it won't be seen by server
			s.addProperty("iforgotTimestamp", 0);
			s.addProperty(Config._PASSWORD, Utils.bcrypt(newpass));
			pc.update(s);
			return true;
		}
		return false;
	}

	private boolean isValidResetToken(Sysprop s, String key, String token) {
		if (StringUtils.isBlank(token)) {
			return false;
		}
		if (s != null && s.hasProperty(key)) {
			String storedToken = (String) s.getProperty(key);
			// tokens expire afer a reasonably short period ~ 30 mins
			long timeout = (long) CONF.passwordResetTimeoutSec() * 1000L;
			if (StringUtils.equals(storedToken, token) && (s.getUpdated() + timeout) > Utils.timestamp()) {
				return true;
			} else {
				logger.info("User {} tried to reset password with an expired reset token.", s.getId());
			}
		}
		return false;
	}

	private void triggerLoginEvent(User u, HttpServletRequest req) {
		if (req != null && u != null) {
			Profile authUser = utils.getAuthUser(req);
			Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(authUser, false));
			Map<String, String> headers = new HashMap<>();
			headers.put(HttpHeaders.REFERER, req.getHeader(HttpHeaders.REFERER));
			headers.put(HttpHeaders.USER_AGENT, req.getHeader(HttpHeaders.USER_AGENT));
			headers.put("User-IP", req.getRemoteAddr());
			payload.put("user", u);
			payload.put("headers", headers);
			utils.triggerHookEvent("user.signin", payload);
		}
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Tag;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import static com.erudika.scoold.ScooldServer.TAGSLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/tags")
public class TagsController {

	private static final Logger logger = LoggerFactory.getLogger(TagsController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public TagsController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public String get(@RequestParam(required = false, defaultValue = "count") String sortby,
			HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);

		Profile authUser = utils.getAuthUser(req);
		String currentSpace = utils.getSpaceIdFromCookie(authUser, req);
		List<Tag> tagslist = Collections.emptyList();
		if (utils.canAccessSpace(authUser, currentSpace)) {
			tagslist = pc.findTags("", itemcount);
		} else if (!utils.isDefaultSpacePublic()) {
			return "redirect:" + SIGNINLINK + "?returnto=" + TAGSLINK;
		}

		model.addAttribute("path", "tags.vm");
		model.addAttribute("title", utils.getLang(req).get("tags.title"));
		model.addAttribute("tagsSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("tagslist", tagslist);
		return "base";
	}

	@PostMapping("/create")
	public String create(@RequestParam String tags, HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isMod(authUser)) {
			Set<Tag> tagz = Arrays.asList(StringUtils.split(tags, ",", 50)).stream().limit(50).
					map(t -> new Tag(t)).collect(Collectors.toSet());
			tagz.removeAll(pc.readAll(tagz.stream().map(t -> t.getId()).collect(Collectors.toList())));
			pc.createAll(new ArrayList<>(tagz));
		}
		return "redirect:" + TAGSLINK + "?success=true&code=done";
	}

	@PostMapping
	public String rename(@RequestParam String tag, @RequestParam String newtag, @RequestParam String description,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		int count = 0;
		if (utils.isMod(authUser)) {
			Tag updated;
			Tag oldTag = new Tag(tag);
			Tag newTag = new Tag(newtag);
			Tag t = pc.read(Utils.type(Tag.class), oldTag.getId());
			if (t != null && !oldTag.getTag().equals(newTag.getTag())) {
				if (oldTag.getTag().equals(newTag.getTag())) {
					t.setCount(pc.getCount(Utils.type(Question.class),
							Collections.singletonMap(Config._TAGS, oldTag.getTag())).intValue());
					updated = pc.update(t);
				} else {
					pc.delete(t);
					t.setId(newtag);
					logger.info("User {} ({}) is renaming tag '{}' to '{}'.",
							authUser.getName(), authUser.getCreatorid(), oldTag.getTag(), t.getTag());

					t.setCount(pc.getCount(Utils.type(Question.class),
							Collections.singletonMap(Config._TAGS, newTag.getTag())).intValue());
					pc.updateAllPartially((toUpdate, pager) -> {
						List<Question> questionslist = pc.findTagged(Utils.type(Question.class), new String[]{oldTag.getTag()}, pager);
						for (Question q : questionslist) {
							t.setCount(t.getCount() + 1);
							q.setTags(Optional.ofNullable(q.getTags()).orElse(Collections.emptyList()).stream().
									map(ts -> {
										if (ts.equals(newTag.getTag())) {
											t.setCount(t.getCount() - 1);
										}
										return ts.equals(oldTag.getTag()) ? t.getTag() : ts;
									}).distinct().
									collect(Collectors.toList()));
							logger.debug("Updated {} out of {} questions with new tag {}.",
									questionslist.size(), pager.getCount(), t.getTag());
							Map<String, Object> post = new HashMap<>();
							post.put(Config._ID, q.getId());
							post.put(Config._TAGS, q.getTags());
							toUpdate.add(post);
						}
						return questionslist;
					});
					updated = pc.create(t); // overwrite new tag object
				}
				model.addAttribute("tag", updated);
				count = t.getCount();
			} else if (t != null && !StringUtils.equals(oldTag.getDescription(), description)) {
				t.setDescription(description);
				updated = pc.update(t);
				model.addAttribute("tag", updated);
				count = t.getCount();
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			res.setContentType("application/json");
			try {
				res.getWriter().println("{\"count\":" + count + ", \"tag\":\"" + new Tag(newtag).getTag() + "\"}");
			} catch (IOException ex) { }
			return "blank";
		} else {
			return "redirect:" + TAGSLINK + "?" + req.getQueryString();
		}
	}

	@PostMapping("/delete")
	public String delete(@RequestParam String tag, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isMod(authUser)) {
			Tag t = pc.read(Utils.type(Tag.class), new Tag(tag).getId());
			if (t != null) {
				pc.delete(t);
				logger.info("User {} ({}) deleted tag '{}'.",
						authUser.getName(), authUser.getCreatorid(), t.getTag());

				pc.updateAllPartially((toUpdate, pager) -> {
					List<Question> questionslist = pc.findTagged(Utils.type(Question.class), new String[]{t.getTag()}, pager);
					for (Question q : questionslist) {
						t.setCount(t.getCount() + 1);
						q.setTags(Optional.ofNullable(q.getTags()).orElse(Collections.emptyList()).stream().
								filter(ts -> !ts.equals(t.getTag())).distinct().collect(Collectors.toList()));
						logger.debug("Removed tag {} from {} out of {} questions.",
								t.getTag(), questionslist.size(), pager.getCount());
						Map<String, Object> post = new HashMap<>();
						post.put(Config._ID, q.getId());
						post.put(Config._TAGS, q.getTags());
						toUpdate.add(post);
					}
					return questionslist;
				});
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "blank";
		} else {
			return "redirect:" + TAGSLINK + "?" + req.getQueryString();
		}
	}

	@ResponseBody
	@GetMapping(path = "/{keyword}", produces = MediaType.APPLICATION_JSON)
	public List<?> findTags(@PathVariable String keyword) {
		return pc.findTags(keyword, new Pager(10));
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.Sysprop;
import com.erudika.para.core.utils.Para;
import static com.erudika.scoold.ScooldServer.PRIVACYLINK;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/privacy")
public class PrivacyController {

	private final ScooldUtils utils;

	@Inject
	public PrivacyController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		model.addAttribute("path", "privacy.vm");
		model.addAttribute("title", utils.getLang(req).get("privacy.title"));
		model.addAttribute("privacyhtml", utils.getParaClient().read("template" + Para.getConfig().separator() + "privacy"));
		return "base";
	}

	@PostMapping
	public String edit(@RequestParam String privacyhtml, HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req) || !utils.isAdmin(utils.getAuthUser(req))) {
			return "redirect:" + PRIVACYLINK;
		}
		Sysprop privacy = new Sysprop("template" + Para.getConfig().separator() + "privacy");
		if (StringUtils.isBlank(privacyhtml)) {
			utils.getParaClient().delete(privacy);
		} else {
			privacy.addProperty("html", privacyhtml);
			utils.getParaClient().create(privacy);
		}
		return "redirect:" + PRIVACYLINK;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class BadRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BadRequestException() {
		super("400 Bad request");
	}

	public BadRequestException(String msg) {
		super(msg);
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Report.ReportType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.ConnectException;
import java.util.Collections;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@Named
public class ScooldRequestInterceptor implements HandlerInterceptor {

	public static final Logger logger = LoggerFactory.getLogger(ScooldRequestInterceptor.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final ScooldUtils utils;

	@Inject
	public ScooldRequestInterceptor(ScooldUtils utils) {
		this.utils = utils;
		ScooldUtils.setInstance(utils);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (utils == null) {
			throw new IllegalStateException("ScooldUtils not initialized properly.");
		}
		boolean isApiRequest = utils.isApiRequest(request);
		try {
			request.setAttribute(AUTH_USER_ATTRIBUTE, utils.checkAuth(request, response));
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException || e.getMessage().contains("Connection refused")) {
				//response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()); // breaks site
				logger.error("No connection to Para backend.", e.getMessage());
			} else if (e instanceof UnauthorizedException && isApiRequest) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			} else {
				logger.error("Auth check failed:", e);
			}
			if (isApiRequest) {
				ParaObjectUtils.getJsonWriter().writeValue(response.getWriter(),
						Collections.singletonMap("error", "Unauthenticated request! " + e.getMessage()));
				return false;
			}
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		if (modelAndView == null || StringUtils.startsWith(modelAndView.getViewName(), "redirect:")) {
			return; // skip if redirect
		}
		// Misc
		modelAndView.addObject("HOMEPAGE", HOMEPAGE);
		modelAndView.addObject("APPNAME", CONF.appName());
		modelAndView.addObject("CDN_URL", CONF.cdnUrl());
		modelAndView.addObject("IN_PRODUCTION", CONF.inProduction());
		modelAndView.addObject("IN_DEVELOPMENT", !CONF.inProduction());
		modelAndView.addObject("MAX_ITEMS_PER_PAGE", CONF.maxItemsPerPage());
		modelAndView.addObject("SESSION_TIMEOUT_SEC", CONF.sessionTimeoutSec());
		modelAndView.addObject("TOKEN_PREFIX", TOKEN_PREFIX);
		modelAndView.addObject("CONTEXT_PATH", CONF.serverContextPath());
		modelAndView.addObject("FB_APP_ID", CONF.facebookAppId());
		modelAndView.addObject("GMAPS_API_KEY", CONF.googleMapsApiKey());
		modelAndView.addObject("IMGUR_CLIENT_ID", CONF.imgurClientId());
		modelAndView.addObject("IMGUR_ENABLED", ScooldUtils.isImgurAvatarRepositoryEnabled());
		modelAndView.addObject("CLOUDINARY_ENABLED", ScooldUtils.isCloudinaryAvatarRepositoryEnabled());
		modelAndView.addObject("RTL_ENABLED", utils.isLanguageRTL(utils.getCurrentLocale(utils.getLanguageCode(request)).getLanguage()));
		modelAndView.addObject("MAX_TAGS_PER_POST", CONF.maxTagsPerPost());
		modelAndView.addObject("includeHighlightJS", CONF.codeHighlightingEnabled());
		modelAndView.addObject("isAjaxRequest", utils.isAjaxRequest(request));
		modelAndView.addObject("reportTypes", ReportType.values());
		modelAndView.addObject("returnto", StringUtils.removeStart(request.getRequestURI(), CONF.serverContextPath()));
		modelAndView.addObject("rev", StringUtils.substring(Utils.md5(Version.getVersion() + CONF.paraSecretKey()), 0, 12));
		// Configurable constants
		modelAndView.addObject("MAX_PAGES", CONF.maxPages());
		modelAndView.addObject("MAX_TEXT_LENGTH", CONF.maxPostLength());
		modelAndView.addObject("MAX_TAGS_PER_POST", CONF.maxTagsPerPost());
		modelAndView.addObject("MAX_REPLIES_PER_POST", CONF.maxRepliesPerPost());
		modelAndView.addObject("MAX_FAV_TAGS", CONF.maxFavoriteTags());
		modelAndView.addObject("MIN_PASS_LENGTH", CONF.minPasswordLength());
		modelAndView.addObject("ANSWER_VOTEUP_REWARD_AUTHOR", CONF.answerVoteupRewardAuthor());
		modelAndView.addObject("QUESTION_VOTEUP_REWARD_AUTHOR", CONF.questionVoteupRewardAuthor());
		modelAndView.addObject("VOTEUP_REWARD_AUTHOR", CONF.voteupRewardAuthor());
		modelAndView.addObject("ANSWER_APPROVE_REWARD_AUTHOR", CONF.answerApprovedRewardAuthor());
		modelAndView.addObject("ANSWER_APPROVE_REWARD_VOTER", CONF.answerApprovedRewardVoter());
		modelAndView.addObject("POST_VOTEDOWN_PENALTY_AUTHOR", CONF.postVotedownPenaltyAuthor());
		modelAndView.addObject("POST_VOTEDOWN_PENALTY_VOTER", CONF.postVotedownPenaltyVoter());
		modelAndView.addObject("VOTER_IFHAS", CONF.voterIfHasRep());
		modelAndView.addObject("COMMENTATOR_IFHAS", CONF.commentatorIfHasRep());
		modelAndView.addObject("CRITIC_IFHAS", CONF.criticIfHasRep());
		modelAndView.addObject("SUPPORTER_IFHAS", CONF.supporterIfHasRep());
		modelAndView.addObject("GOODQUESTION_IFHAS", CONF.goodQuestionIfHasRep());
		modelAndView.addObject("GOODANSWER_IFHAS", CONF.goodAnswerIfHasRep());
		modelAndView.addObject("ENTHUSIAST_IFHAS", CONF.enthusiastIfHasRep());
		modelAndView.addObject("FRESHMAN_IFHAS", CONF.freshmanIfHasRep());
		modelAndView.addObject("SCHOLAR_IFHAS", CONF.scholarIfHasRep());
		modelAndView.addObject("TEACHER_IFHAS", CONF.teacherIfHasRep());
		modelAndView.addObject("PROFESSOR_IFHAS", CONF.professorIfHasRep());
		modelAndView.addObject("GEEK_IFHAS", CONF.geekIfHasRep());
		// Cookies
		modelAndView.addObject("localeCookieName", CONF.localeCookie());
		// Paths
		Profile authUser = (Profile) request.getAttribute(AUTH_USER_ATTRIBUTE);
		modelAndView.addObject("imageslink", CONF.imagesLink()); // do not add context path prefix!
		modelAndView.addObject("scriptslink", CONF.scriptsLink()); // do not add context path prefix!
		modelAndView.addObject("styleslink", CONF.stylesLink()); // do not add context path prefix!
		modelAndView.addObject("peoplelink", CONF.serverContextPath() + PEOPLELINK);
		modelAndView.addObject("profilelink", CONF.serverContextPath() + PROFILELINK);
		modelAndView.addObject("searchlink", CONF.serverContextPath() + SEARCHLINK);
		modelAndView.addObject("signinlink", CONF.serverContextPath() + SIGNINLINK);
		modelAndView.addObject("signoutlink", CONF.serverContextPath() + SIGNOUTLINK);
		modelAndView.addObject("aboutlink", CONF.serverContextPath() + ABOUTLINK);
		modelAndView.addObject("privacylink", CONF.serverContextPath() + PRIVACYLINK);
		modelAndView.addObject("termslink", CONF.serverContextPath() + TERMSLINK);
		modelAndView.addObject("tagslink", CONF.serverContextPath() + TAGSLINK);
		modelAndView.addObject("settingslink", CONF.serverContextPath() + SETTINGSLINK);
		modelAndView.addObject("reportslink", CONF.serverContextPath() + REPORTSLINK);
		modelAndView.addObject("adminlink", CONF.serverContextPath() + ADMINLINK);
		modelAndView.addObject("votedownlink", CONF.serverContextPath() + VOTEDOWNLINK);
		modelAndView.addObject("voteuplink", CONF.serverContextPath() + VOTEUPLINK);
		modelAndView.addObject("questionlink", CONF.serverContextPath() + QUESTIONLINK);
		modelAndView.addObject("questionslink", CONF.serverContextPath() + QUESTIONSLINK);
		modelAndView.addObject("commentlink", CONF.serverContextPath() + COMMENTLINK);
		modelAndView.addObject("postlink", CONF.serverContextPath() + POSTLINK);
		modelAndView.addObject("revisionslink", CONF.serverContextPath() + REVISIONSLINK);
		modelAndView.addObject("feedbacklink", CONF.serverContextPath() + FEEDBACKLINK);
		modelAndView.addObject("languageslink", CONF.serverContextPath() + LANGUAGESLINK);
		modelAndView.addObject("apidocslink", CONF.serverContextPath() + APIDOCSLINK);
		// Visual customization
		modelAndView.addObject("navbarFixedClass", CONF.fixedNavEnabled() ? "navbar-fixed" : "none");
		modelAndView.addObject("showBranding", CONF.scooldBrandingEnabled());
		modelAndView.addObject("logoUrl", utils.getLogoUrl(authUser, request));
		modelAndView.addObject("logoWidth", CONF.logoWidth());
		modelAndView.addObject("stylesheetUrl", CONF.stylesheetUrl());
		modelAndView.addObject("darkStylesheetUrl", CONF.darkStylesheetUrl());
		modelAndView.addObject("faviconUrl", CONF.faviconUrl());
		modelAndView.addObject("inlineUserCSS", utils.getInlineCSS());
		modelAndView.addObject("compactViewEnabled", "true".equals(HttpUtils.getCookieValue(request, "questions-view-compact")));
		modelAndView.addObject("compactUsersViewEnabled", "true".equals(HttpUtils.getCookieValue(request, "users-view-compact")));
		modelAndView.addObject("darkModeEnabled", utils.isDarkModeEnabled(authUser, request));
		// Auth & Badges
		modelAndView.addObject("authenticated", authUser != null);
		modelAndView.addObject("canComment", utils.canComment(authUser, request));
		modelAndView.addObject("isMod", utils.isMod(authUser));
		modelAndView.addObject("isAdmin", utils.isAdmin(authUser));
		modelAndView.addObject("utils", Utils.getInstance());
		modelAndView.addObject("scooldUtils", utils);
		modelAndView.addObject("authUser", authUser);
		modelAndView.addObject("badgelist", utils.checkForBadges(authUser, request));
		modelAndView.addObject("request", request);
		// Spaces
		String currentSpace = utils.getSpaceIdFromCookie(authUser, request);
		modelAndView.addObject("currentSpace", utils.isDefaultSpace(currentSpace) ? "" : currentSpace);
		// Language
		Locale currentLocale = utils.getCurrentLocale(utils.getLanguageCode(request));
		modelAndView.addObject("currentLocale", currentLocale);
		modelAndView.addObject("lang", utils.getLang(currentLocale));
		modelAndView.addObject("langDirection", utils.isLanguageRTL(currentLocale.getLanguage()) ? "RTL" : "LTR");
		// Pagination
		modelAndView.addObject("numericPaginationEnabled", CONF.numericPaginationEnabled());
		// Markdown with HTML
		modelAndView.addObject("htmlInMarkdownEnabled", CONF.htmlInMarkdownEnabled());
		// check for AJAX pagination requests
		if (utils.isAjaxRequest(request) && (utils.param(request, "page") || utils.param(request, "page1") ||
				utils.param(request, "page2") || utils.param(request, "page3"))) {
			modelAndView.setViewName("pagination"); // switch to page fragment view
		}
		// External scripts
		modelAndView.addObject("externalScripts", utils.getExternalScripts());
		// External styles
		modelAndView.addObject("externalStyles", utils.getExternalStyles());
		// GDPR
		modelAndView.addObject("cookieConsentGiven", utils.cookieConsentGiven(request));
		// CSP nonce
		String cspNonce = utils.getCSPNonce();
		modelAndView.addObject("cspNonce", cspNonce);
		// CSP, HSTS, etc, headers. See https://securityheaders.com
		utils.setSecurityHeaders(cspNonce, request, response);
		// default metadata for social meta tags
		if (StringUtils.isBlank(modelAndView.getModel().getOrDefault("title", "") + "")) {
			modelAndView.addObject("title", CONF.appName());
		}
		if (StringUtils.isBlank(modelAndView.getModel().getOrDefault("description", "") + "")) {
			modelAndView.addObject("description", CONF.metaDescription());
		}
		if (StringUtils.isBlank(modelAndView.getModel().getOrDefault("keywords", "") + "")) {
			modelAndView.addObject("keywords", CONF.metaKeywords());
		}
		if (StringUtils.isBlank(modelAndView.getModel().getOrDefault("ogimage", "") + "")) {
			modelAndView.addObject("ogimage", CONF.metaAppIconUrl());
		}
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import java.net.URI;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * HttpRequestMethodNotSupportedException handler - suppress spammy log messages.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RequestNotSupportedExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
		if (!CollectionUtils.isEmpty(supportedMethods)) {
			headers.setAllow(supportedMethods);
		}
		return new ResponseEntity<>(null, headers, status);
	}

	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(
			NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT).location(URI.create("/not-found")).build();
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Votable;
import com.erudika.para.core.Vote;
import com.erudika.para.core.Webhook;
import com.erudika.para.core.email.Emailer;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.para.core.validation.ValidationUtils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import static com.erudika.scoold.core.Post.ALL_MY_SPACES;
import static com.erudika.scoold.core.Post.DEFAULT_SPACE;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.ENTHUSIAST;
import static com.erudika.scoold.core.Profile.Badge.TEACHER;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Revision;
import static com.erudika.scoold.utils.HttpUtils.getCookieValue;
import com.erudika.scoold.utils.avatars.AvatarFormat;
import com.erudika.scoold.utils.avatars.AvatarRepository;
import com.erudika.scoold.utils.avatars.AvatarRepositoryProxy;
import com.erudika.scoold.utils.avatars.GravatarAvatarGenerator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.thauvin.erik.akismet.Akismet;
import net.thauvin.erik.akismet.AkismetComment;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@Named
public final class ScooldUtils {

	private static final Logger logger = LoggerFactory.getLogger(ScooldUtils.class);
	private static final Map<String, String> FILE_CACHE = new ConcurrentHashMap<String, String>();
	private static final Set<String> APPROVED_DOMAINS = new HashSet<>();
	private static final Set<String> ADMINS = new HashSet<>();
	private static final String EMAIL_ALERTS_PREFIX = "email-alerts" + Para.getConfig().separator();

	private static final Profile API_USER;
	private static final Set<String> HOOK_EVENTS;
	private static final Map<String, String> WHITELISTED_MACROS;
	private static final Map<String, Object> API_KEYS = new LinkedHashMap<>(); // jti => jwt

	private Set<Sysprop> allSpaces;
	private Set<String> autoAssignedSpacesFromConfig;
	private long lastSpacesCountTimestamp;
	private int spacesCount = 0;

	private static final ScooldConfig CONF = new ScooldConfig();

	static {
		API_USER = new Profile("1", "System");
		API_USER.setVotes(1);
		API_USER.setCreatorid("1");
		API_USER.setTimestamp(Utils.timestamp());
		API_USER.setGroups(User.Groups.ADMINS.toString());

		HOOK_EVENTS = new HashSet<>(Arrays.asList(
				"question.create",
				"question.close",
				"question.view",
				"question.approve",
				"answer.create",
				"answer.accept",
				"answer.approve",
				"report.create",
				"comment.create",
				"user.signin",
				"user.signup",
				"user.search",
				"revision.restore"));

		WHITELISTED_MACROS = new HashMap<String, String>();
		WHITELISTED_MACROS.put("spaces", "#spacespage($spaces)");
		WHITELISTED_MACROS.put("webhooks", "#webhookspage($webhooks)");
		WHITELISTED_MACROS.put("comments", "#commentspage($commentslist)");
		WHITELISTED_MACROS.put("simplecomments", "#simplecommentspage($commentslist)");
		WHITELISTED_MACROS.put("postcomments", "#commentspage($showpost.comments)");
		WHITELISTED_MACROS.put("replies", "#answerspage($answerslist $showPost)");
		WHITELISTED_MACROS.put("feedback", "#questionspage($feedbacklist)");
		WHITELISTED_MACROS.put("people", "#peoplepage($userlist)");
		WHITELISTED_MACROS.put("questions", "#questionspage($questionslist)");
		WHITELISTED_MACROS.put("compactanswers", "#compactanswerspage($answerslist)");
		WHITELISTED_MACROS.put("answers", "#answerspage($answerslist)");
		WHITELISTED_MACROS.put("reports", "#reportspage($reportslist)");
		WHITELISTED_MACROS.put("revisions", "#revisionspage($revisionslist $showPost)");
		WHITELISTED_MACROS.put("tags", "#tagspage($tagslist)");
	}

	private final ParaClient pc;
	private final ParaClient pcThrows;
	private final LanguageUtils langutils;
	private final AvatarRepository avatarRepository;
	private final GravatarAvatarGenerator gravatarAvatarGenerator;
	private static ScooldUtils instance;
	private Sysprop customTheme;
	@Inject private Emailer emailer;

	public static final int MAX_SPACES = 10; // Hey! It's cool to edit this, but please consider buying Scoold Pro! :)

	@Inject
	public ScooldUtils(ParaClient pc, LanguageUtils langutils, AvatarRepositoryProxy avatarRepository,
			GravatarAvatarGenerator gravatarAvatarGenerator) {
		this.pc = pc;
		this.langutils = langutils;
		this.avatarRepository = avatarRepository;
		this.gravatarAvatarGenerator = gravatarAvatarGenerator;
		this.pcThrows = new ParaClient(CONF.paraAccessKey(), CONF.paraSecretKey());
		API_USER.setPicture(avatarRepository.getAnonymizedLink(CONF.supportEmail()));
		setParaEndpointAndApiPath(pcThrows);
		pcThrows.throwExceptionOnHTTPError(true);
	}

	public ParaClient getParaClient() {
		return pc;
	}

	public LanguageUtils getLangutils() {
		return langutils;
	}

	public static ScooldUtils getInstance() {
		return instance;
	}

	static void setInstance(ScooldUtils instance) {
		ScooldUtils.instance = instance;
	}

	public static ScooldConfig getConfig() {
		return CONF;
	}

	static {
		// multiple domains/admins are allowed only in Scoold PRO
		String approvedDomain = StringUtils.substringBefore(CONF.approvedDomainsForSignups(), ",");
		if (!StringUtils.isBlank(approvedDomain)) {
			APPROVED_DOMAINS.add(approvedDomain.toLowerCase());
		}
		// multiple admins are allowed only in Scoold PRO
		String admin = StringUtils.substringBefore(CONF.admins(), ",");
		if (!StringUtils.isBlank(admin)) {
			ADMINS.add(admin);
		}
	}

	public static void setParaEndpointAndApiPath(ParaClient pc) {
		try {
			URL endpoint = new URI(CONF.paraEndpoint()).toURL();
			if (!StringUtils.isBlank(endpoint.getPath()) && !"/".equals(endpoint.getPath())) {
				// support Para deployed under a specific context path
				pc.setEndpoint(StringUtils.removeEnd(CONF.paraEndpoint(), endpoint.getPath()));
				pc.setApiPath(StringUtils.stripEnd(endpoint.getPath(), "/") + pc.getApiPath());
			} else {
				pc.setEndpoint(CONF.paraEndpoint());
			}
		} catch (Exception e) {
			logger.error("Invalid Para endpoint URL: {}", CONF.paraEndpoint());
		}
	}

	public static void tryConnectToPara(Callable<Boolean> callable) {
		retryConnection(callable, 0);
	}

	private static void retryConnection(Callable<Boolean> callable, int retryCount) {
		try {
			if (!callable.call()) {
				throw new Exception();
			} else if (retryCount > 0) {
				logger.info("Connected to Para backend.");
			}
		} catch (Exception e) {
			int maxRetries = CONF.paraConnectionRetryAttempts();
			int retryInterval = CONF.paraConnectionRetryIntervalSec();
			int count = ++retryCount;
			logger.error("No connection to Para backend. Retrying connection in {}s (attempt {} of {})...",
					retryInterval, count, maxRetries);
			if (maxRetries < 0 || retryCount < maxRetries) {
				Para.asyncExecute(() -> {
					try {
						Thread.sleep(retryInterval * 1000L);
					} catch (InterruptedException ex) {
						logger.error(null, ex);
						Thread.currentThread().interrupt();
					}
					retryConnection(callable, count);
				});
			}
		}
	}

	public ParaObject checkAuth(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		Profile authUser = null;
		String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
		if (isApiRequest(req)) {
			return checkApiAuth(req);
		} else if (jwt != null && !StringUtils.endsWithAny(req.getServletPath(),
				".js", ".css", ".svg", ".png", ".jpg", ".ico", ".gif", ".woff2", ".woff", "people/avatar", "/two-factor")) {
			User u = pc.me(jwt);
			if (u != null && isEmailDomainApproved(u.getEmail())) {
				authUser = getOrCreateProfile(u, req);
				authUser.setUser(u);
				authUser.setOriginalPicture(u.getPicture());
				authUser.setCurrentSpace(getSpaceIdFromCookie(authUser, req));
				boolean updatedRank = promoteOrDemoteUser(authUser, u);
				boolean updatedProfile = updateProfilePictureAndName(authUser, u);
				if (updatedRank || updatedProfile) {
					authUser.update();
				}
				if (u.getTwoFA() && !HttpUtils.isValid2FACookie(u, getUnverifiedClaimsFromJWT(jwt).getIssueTime(), req, res)) {
					res.sendRedirect(CONF.serverUrl() + CONF.serverContextPath() + SIGNINLINK + "/two-factor");
					return null;
				}
			} else {
				clearSession(req, res);
				logger.info("Invalid JWT found in cookie {}.", CONF.authCookie());
				res.sendRedirect(CONF.serverUrl() + CONF.serverContextPath() + SIGNINLINK + "?code=3&error=true");
				return null;
			}
		}
		return authUser;
	}

	private ParaObject checkApiAuth(HttpServletRequest req) {
		if (req.getServletPath().equals("/api")) {
			return null;
		}
		String apiKeyJWT = StringUtils.removeStart(req.getHeader(HttpHeaders.AUTHORIZATION), "Bearer ");
		if (req.getServletPath().equals("/api/ping")) {
			return API_USER;
		} else if (req.getServletPath().equals("/api/stats") && isValidJWToken(apiKeyJWT)) {
			return API_USER;
		} else if (req.getServletPath().startsWith("/api/config") && isValidJWToken(apiKeyJWT)) {
			return API_USER;
		} else if (!isApiEnabled() || StringUtils.isBlank(apiKeyJWT) || !isValidJWToken(apiKeyJWT)) {
			throw new UnauthorizedException();
		}
		return API_USER;
	}

	private boolean promoteOrDemoteUser(Profile authUser, User u) {
		if (authUser != null && authUser.getEditorRoleEnabled()) {
			if (!isAdmin(authUser) && isRecognizedAsAdmin(u)) {
				logger.info("User '{}' with id={} promoted to admin.", u.getName(), authUser.getId());
				authUser.setGroups(User.Groups.ADMINS.toString());
				return true;
			} else if (isAdmin(authUser) && !isRecognizedAsAdmin(u)) {
				logger.info("User '{}' with id={} demoted to regular user.", u.getName(), authUser.getId());
				authUser.setGroups(User.Groups.USERS.toString());
				return true;
			} else if (!isMod(authUser) && u.isModerator()) {
				authUser.setGroups(User.Groups.MODS.toString());
				return true;
			}
		}
		return false;
	}

	private Profile getOrCreateProfile(User u, HttpServletRequest req) {
		Profile authUser;
		try {
			authUser = pcThrows.read(Profile.id(u.getId())); // what if this request fails (server down, OS frozen, etc)?
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			authUser = pcThrows.read(Profile.id(u.getId())); // try again
			if (authUser != null) {
				return authUser;
			}
		}
		if (authUser == null) {
			authUser = Profile.fromUser(u);
			authUser.create();
			if (!u.getIdentityProvider().equals("generic")) {
				sendWelcomeEmail(u, false, req);
			}
			Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(authUser, false));
			payload.put("user", u);
			triggerHookEvent("user.signup", payload);
			logger.info("Created new user '{}' with id={}, groups={}, spaces={}.",
					u.getName(), authUser.getId(), authUser.getGroups(), authUser.getSpaces());
		}
		return authUser;
	}

	private boolean updateProfilePictureAndName(Profile authUser, User u) {
		boolean update = false;
		if (!StringUtils.equals(u.getPicture(), authUser.getPicture())
				&& !gravatarAvatarGenerator.isLink(authUser.getPicture())
				&& !CONF.avatarEditsEnabled()) {
			authUser.setPicture(u.getPicture());
			update = true;
		}
		if (!CONF.nameEditsEnabled() &&	!StringUtils.equals(u.getName(), authUser.getName())) {
			authUser.setName(StringUtils.abbreviate(u.getName(), 256));
			update = true;
		}
		if (!StringUtils.equals(u.getName(), authUser.getOriginalName())) {
			authUser.setOriginalName(u.getName());
			update = true;
		}
		if (authUser.isComplete()) {
			update = addBadgeOnce(authUser, Profile.Badge.NICEPROFILE, authUser.isComplete()) || update;
		}
		return update;
	}

	public boolean isDarkModeEnabled(Profile authUser, HttpServletRequest req) {
		return (authUser != null && authUser.getDarkmodeEnabled()) ||
				"1".equals(HttpUtils.getCookieValue(req, "dark-mode"));
	}

	private String getDefaultEmailSignature(String defaultText) {
		String template = CONF.emailsDefaultSignatureText(defaultText);
		return Utils.formatMessage(template, CONF.appName());
	}

	public void sendWelcomeEmail(User user, boolean verifyEmail, HttpServletRequest req) {
		// send welcome email notification
		if (user != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = Utils.formatMessage(lang.get("signin.welcome"), CONF.appName());
			String body1 = Utils.formatMessage(CONF.emailsWelcomeText1(lang), CONF.appName());
			String body2 = CONF.emailsWelcomeText2(lang);
			String body3 = getDefaultEmailSignature(CONF.emailsWelcomeText3(lang));

			if (verifyEmail && !user.getActive() && !StringUtils.isBlank(user.getIdentifier())) {
				Sysprop s = pc.read(user.getIdentifier());
				if (s != null) {
					String token = Utils.base64encURL(Utils.generateSecurityToken().getBytes());
					s.addProperty(Config._EMAIL_TOKEN, token);
					pc.update(s);
					token = CONF.serverUrl() + CONF.serverContextPath() + SIGNINLINK + "/register?id=" + user.getId() + "&token=" + token;
					body3 = "<b><a href=\"" + token + "\">" + lang.get("signin.welcome.verify") + "</a></b><br><br>" + body3;
				}
			}

			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", Utils.formatMessage(lang.get("signin.welcome.title"), escapeHtml(user.getName())));
			model.put("body", body1 + body2 + body3);
			emailer.sendEmail(Arrays.asList(user.getEmail()), subject, compileEmailTemplate(model));
		}
	}

	public void sendVerificationEmail(Sysprop identifier, String newEmail, String redirectUrl, HttpServletRequest req) {
		if (identifier != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = CONF.appName() + " - " + (StringUtils.isBlank(newEmail) ? lang.get("msgcode.6") :
					lang.get("signin.verify.change") + newEmail);
			String body = getDefaultEmailSignature(CONF.emailsWelcomeText3(lang));
			redirectUrl = StringUtils.isBlank(redirectUrl) ? SIGNINLINK + "/register" : redirectUrl;

			String token1 = Utils.base64encURL(Utils.generateSecurityToken().getBytes());
			String token2 = Utils.base64encURL(Utils.generateSecurityToken().getBytes());
			identifier.addProperty(Config._EMAIL_TOKEN, token1);
			identifier.addProperty(Config._EMAIL_TOKEN + "2", token2);
			identifier.addProperty("confirmationTimestamp", Utils.timestamp());
			pc.update(identifier);
			token1 = CONF.serverUrl() + CONF.serverContextPath() + redirectUrl + "?id=" +
					identifier.getCreatorid() + "&token=" + token1;
			token2 = CONF.serverUrl() + CONF.serverContextPath() + redirectUrl + "?id=" +
					identifier.getCreatorid() + "&token2=" + token2;

			body = "<b><a href=\"{0}\">" + (StringUtils.isBlank(newEmail) ? lang.get("signin.welcome.verify") :
					lang.get("signin.verify.change")) + " " + newEmail + "</a></b><br><br>" + body;

			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", lang.get("hello"));
			model.put("body", Utils.formatMessage(body, token1));

			emailer.sendEmail(Arrays.asList(identifier.getId()), subject, compileEmailTemplate(model));

			if (!StringUtils.isBlank(newEmail)) {
				model.put("body", Utils.formatMessage(body, token2));
				emailer.sendEmail(Arrays.asList(newEmail), subject, compileEmailTemplate(model));
			}
		}
	}

	public void sendPasswordResetEmail(String email, String token, HttpServletRequest req) {
		if (email != null && token != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String url = CONF.serverUrl() + CONF.serverContextPath() + SIGNINLINK + "/iforgot?email=" + email + "&token=" + token;
			String subject = lang.get("iforgot.title");
			String body1 = lang.get("notification.iforgot.body1") + "<br><br>";
			String body2 = Utils.formatMessage("<b><a href=\"{0}\">" + lang.get("notification.iforgot.body2") +
					"</a></b><br><br>", url);
			String body3 = getDefaultEmailSignature(lang.get("notification.signature") + "<br><br>");

			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", lang.get("hello"));
			model.put("body", body1 + body2 + body3);
			emailer.sendEmail(Arrays.asList(email), subject, compileEmailTemplate(model));
		}
	}

	@SuppressWarnings("unchecked")
	public void subscribeToNotifications(String email, String channelId) {
		if (!StringUtils.isBlank(email) && !StringUtils.isBlank(channelId)) {
			Sysprop s = pc.read(channelId);
			if (s == null || !s.hasProperty("emails")) {
				s = new Sysprop(channelId);
				s.addProperty("emails", new LinkedList<>());
			}
			Set<String> emails = new HashSet<>((List<String>) s.getProperty("emails"));
			if (emails.add(email)) {
				s.addProperty("emails", emails);
				pc.create(s);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void unsubscribeFromNotifications(String email, String channelId) {
		if (!StringUtils.isBlank(email) && !StringUtils.isBlank(channelId)) {
			Sysprop s = pc.read(channelId);
			if (s == null || !s.hasProperty("emails")) {
				s = new Sysprop(channelId);
				s.addProperty("emails", new LinkedList<>());
			}
			Set<String> emails = new HashSet<>((List<String>) s.getProperty("emails"));
			if (emails.remove(email)) {
				s.addProperty("emails", emails);
				pc.create(s);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Set<String> getNotificationSubscribers(String channelId) {
		return ((List<String>) Optional.ofNullable(((Sysprop) pc.read(channelId))).
				orElse(new Sysprop()).getProperties().getOrDefault("emails", Collections.emptyList())).
				stream().collect(Collectors.toSet());
	}

	public void unsubscribeFromAllNotifications(Profile p) {
		User u = p.getUser();
		if (u != null) {
			unsubscribeFromNewPosts(u);
		}
	}

	public boolean isEmailDomainApproved(String email) {
		if (StringUtils.isBlank(email)) {
			return false;
		}
		if (!APPROVED_DOMAINS.isEmpty() && !APPROVED_DOMAINS.contains(StringUtils.substringAfter(email, "@").toLowerCase())) {
			logger.warn("Attempted signin from an unknown domain - email {} is part of an unapproved domain.", email);
			return false;
		}
		return true;
	}

	public Object isSubscribedToNewPosts(HttpServletRequest req) {
		if (!isNewPostNotificationAllowed()) {
			return false;
		}

		Profile authUser = getAuthUser(req);
		if (authUser != null) {
			User u = authUser.getUser();
			if (u != null) {
				return getNotificationSubscribers(EMAIL_ALERTS_PREFIX + "new_post_subscribers").contains(u.getEmail());
			}
		}
		return false;
	}

	public void subscribeToNewPosts(User u) {
		if (u != null) {
			subscribeToNotifications(u.getEmail(), EMAIL_ALERTS_PREFIX + "new_post_subscribers");
		}
	}

	public void unsubscribeFromNewPosts(User u) {
		if (u != null) {
			unsubscribeFromNotifications(u.getEmail(), EMAIL_ALERTS_PREFIX + "new_post_subscribers");
		}
	}

	public Object isSubscribedToNewReplies(HttpServletRequest req) {
		if (!isReplyNotificationAllowed()) {
			return false;
		}
		Profile authUser = getAuthUser(req);
		if (authUser != null) {
			User u = authUser.getUser();
			if (u != null) {
				return getNotificationSubscribers(EMAIL_ALERTS_PREFIX + "new_reply_subscribers").contains(u.getEmail());
			}
		}
		return false;
	}

	public void subscribeToNewReplies(User u) {
		if (u != null) {
			subscribeToNotifications(u.getEmail(), EMAIL_ALERTS_PREFIX + "new_reply_subscribers");
		}
	}

	public void unsubscribeFromNewReplies(User u) {
		if (u != null) {
			unsubscribeFromNotifications(u.getEmail(), EMAIL_ALERTS_PREFIX + "new_reply_subscribers");
		}
	}

	private Map<String, Profile> buildProfilesMap(List<User> users) {
		if (users != null && !users.isEmpty()) {
			Map<String, User> userz = users.stream().collect(Collectors.toMap(u -> u.getId(), u -> u));
			List<Profile> profiles = pc.readAll(userz.keySet().stream().
					map(uid -> Profile.id(uid)).collect(Collectors.toList()));
			Map<String, Profile> profilesMap = new HashMap<String, Profile>(users.size());
			profiles.forEach(pr -> profilesMap.put(userz.get(pr.getCreatorid()).getEmail(), pr));
			return profilesMap;
		}
		return Collections.emptyMap();
	}

	private void sendEmailsToSubscribersInSpace(Set<String> emails, String space, String subject, String html) {
		int i = 0;
		int max = CONF.maxItemsPerPage();
		List<String> terms = new ArrayList<>(max);
		for (String email : emails) {
			terms.add(email);
			if (++i == max) {
				emailer.sendEmail(buildProfilesMap(pc.findTermInList(Utils.type(User.class), Config._EMAIL, terms)).
						entrySet().stream().filter(e -> canAccessSpace(e.getValue(), space) &&
								!isIgnoredSpaceForNotifications(e.getValue(), space)).
						map(e -> e.getKey()).collect(Collectors.toList()), subject, html);
				i = 0;
				terms.clear();
			}
		}
		if (!terms.isEmpty()) {
			emailer.sendEmail(buildProfilesMap(pc.findTermInList(Utils.type(User.class), Config._EMAIL, terms)).
					entrySet().stream().filter(e -> canAccessSpace(e.getValue(), space) &&
							!isIgnoredSpaceForNotifications(e.getValue(), space)).
					map(e -> e.getKey()).collect(Collectors.toList()), subject, html);
		}
	}

	private Set<String> getFavTagsSubscribers(List<String> tags) {
		if (tags != null && tags.stream().filter(t -> !StringUtils.isBlank(t)).count() > 0) {
			Set<String> emails = new LinkedHashSet<>();
			pc.readEverything(pager -> {
				List<Profile> profiles = pc.findQuery(Utils.type(Profile.class),
						"properties.favtags:(" + tags.stream().
								map(t -> "\"".concat(t).concat("\"")).distinct().
								collect(Collectors.joining(" ")) + ") AND properties.favtagsEmailsEnabled:true", pager);
				if (!profiles.isEmpty()) {
					List<User> users = pc.readAll(profiles.stream().map(p -> p.getCreatorid()).
							distinct().collect(Collectors.toList()));

					users.stream().forEach(u -> emails.add(u.getEmail()));
				}
				return profiles;
			});
			return emails;
		}
		return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public void sendUpdatedFavTagsNotifications(Post question, List<String> addedTags, HttpServletRequest req) {
		if (!isFavTagsNotificationAllowed()) {
			return;
		}
		// sends a notification to subscibers of a tag if that tag was added to an existing question
		if (question != null && !question.isReply() && addedTags != null && !addedTags.isEmpty()) {
			Profile postAuthor = question.getAuthor(); // the current user - same as utils.getAuthUser(req)
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String name = postAuthor.getName();
			String body = Utils.markdownToHtml(question.getBody());
			String picture = Utils.formatMessage("<img src='{0}' width='25'>", escapeHtmlAttribute(avatarRepository.
					getLink(postAuthor, AvatarFormat.Square25)));
			String postURL = CONF.serverUrl() + question.getPostLink(false, false);
			String tagsString = Optional.ofNullable(question.getTags()).orElse(Collections.emptyList()).stream().
					map(t -> "<span class=\"tag\">" +
							(addedTags.contains(t) ? "<b>" + escapeHtml(t) + "<b>" : escapeHtml(t)) + "</span>").
					collect(Collectors.joining("&nbsp;"));
			String subject = Utils.formatMessage(lang.get("notification.favtags.subject"), name,
					Utils.abbreviate(question.getTitle(), 255));
			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", Utils.formatMessage(lang.get("notification.favtags.heading"), picture, escapeHtml(name)));
			model.put("body", Utils.formatMessage("<h2><a href='{0}'>{1}</a></h2><div>{2}</div><br>{3}",
					postURL, escapeHtml(question.getTitle()), body, tagsString));

			Set<String> emails = getFavTagsSubscribers(addedTags);
			sendEmailsToSubscribersInSpace(emails, question.getSpace(), subject, compileEmailTemplate(model));
		}
	}

	@SuppressWarnings("unchecked")
	public void sendNewPostNotifications(Post question, boolean needApproval, HttpServletRequest req) {
		if (question == null || req.getParameter("notificationsDisabled") != null) {
			return;
		}
		// the current user - same as utils.getAuthUser(req)
		Profile postAuthor = question.getAuthor() != null ? question.getAuthor() : pc.read(question.getCreatorid());
		if (!isNewPostNotificationAllowed()) {
			return;
		}
		boolean awaitingApproval = needApproval;
		Map<String, Object> model = new HashMap<String, Object>();
		Map<String, String> lang = getLang(req);
		String name = postAuthor.getName();
		String body = Utils.markdownToHtml(question.getBody());
		String picture = Utils.formatMessage("<img src='{0}' width='25'>", escapeHtmlAttribute(avatarRepository.
				getLink(postAuthor, AvatarFormat.Square25)));
		String postURL = CONF.serverUrl() + question.getPostLink(false, false);
		String tagsString = Optional.ofNullable(question.getTags()).orElse(Collections.emptyList()).stream().
				map(t -> "<span class=\"tag\">" + escapeHtml(t) + "</span>").
				collect(Collectors.joining("&nbsp;"));
		String subject = Utils.formatMessage(lang.get("notification.newposts.subject"), name,
				Utils.abbreviate(question.getTitle(), 255));
		model.put("subject", escapeHtml((awaitingApproval ? "[" + lang.get("reports.awaitingapproval") + "] " : "") + subject));
		model.put("logourl", getSmallLogoUrl());
		model.put("heading", Utils.formatMessage(lang.get("notification.newposts.heading"), picture, escapeHtml(name)));
		model.put("body", Utils.formatMessage("<h2><a href='{0}'>{1}</a></h2><div>{2}</div><br>{3}",
				postURL, escapeHtml(question.getTitle()), body, tagsString));

		Set<String> emails = new HashSet<String>(getNotificationSubscribers(EMAIL_ALERTS_PREFIX + "new_post_subscribers"));
		emails.addAll(getFavTagsSubscribers(question.getTags()));
		sendEmailsToSubscribersInSpace(emails, question.getSpace(), subject, compileEmailTemplate(model));
		if (!isMod(postAuthor)) {
			createReportCopyOfNotificiation(name, postURL, subject, body, awaitingApproval);
		}

		if (awaitingApproval) {
			Report rep = new Report();
			rep.setName(question.getTitle());
			rep.setContent(Utils.abbreviate(Utils.markdownToHtml(question.getBody()), 2000));
			rep.setParentid(question.getId());
			rep.setDescription(lang.get("reports.awaitingapproval") + (question.isSpam() ? " [spam]" : ""));
			rep.setSubType(question.isSpam() ? Report.ReportType.SPAM : Report.ReportType.OTHER);
			rep.setLink(question.getPostLink(false, false));
			rep.setAuthorName(postAuthor.getName());
			rep.addProperty(lang.get("spaces.title"), getSpaceName(question.getSpace()));
			rep.create();
		}
	}

	public void sendReplyNotifications(Post parentPost, Post reply, boolean needApproval, HttpServletRequest req) {
		// send email notification to author of post except when the reply is by the same person
		if (parentPost == null || reply == null) {
			return;
		}
		Map<String, String> lang = getLang(req);
		Profile replyAuthor = reply.getAuthor(); // the current user - same as utils.getAuthUser(req)
		boolean awaitingApproval = needApproval;
		Map<String, Object> model = new HashMap<String, Object>();
		String name = replyAuthor.getName();
		String body = Utils.markdownToHtml(reply.getBody());
		String picture = Utils.formatMessage("<img src='{0}' width='25'>", escapeHtmlAttribute(avatarRepository.
				getLink(replyAuthor, AvatarFormat.Square25)));
		String postURL = CONF.serverUrl() + parentPost.getPostLink(false, false);
		String subject = Utils.formatMessage(lang.get("notification.reply.subject"), name,
				Utils.abbreviate(reply.getTitle(), 255));
		model.put("subject", escapeHtml((awaitingApproval ? "[" + lang.get("reports.awaitingapproval") + "] " : "") + subject));
		model.put("logourl", getSmallLogoUrl());
		model.put("heading", Utils.formatMessage(lang.get("notification.reply.heading"),
				Utils.formatMessage("<a href='{0}'>{1}</a>", postURL, escapeHtml(parentPost.getTitle()))));
		model.put("body", Utils.formatMessage("<h2>{0} {1}:</h2><div>{2}</div>", picture, escapeHtml(name), body));

		if (!StringUtils.equals(parentPost.getCreatorid(), reply.getCreatorid())) {
			Profile authorProfile = pc.read(parentPost.getCreatorid());
			if (authorProfile != null) {
				User author = authorProfile.getUser();
				if (author != null) {
					if (authorProfile.getReplyEmailsEnabled()) {
						parentPost.addFollower(author);
					}
				}
			}

			if (isReplyNotificationAllowed()) {
				if (parentPost.hasFollowers()) {
					emailer.sendEmail(new ArrayList<String>(parentPost.getFollowers().values()), subject, compileEmailTemplate(model));
				}
				// also notify all mods/admins who wish to monitor all answers
				Set<String> emails = new HashSet<String>(getNotificationSubscribers(EMAIL_ALERTS_PREFIX + "new_reply_subscribers"));
				sendEmailsToSubscribersInSpace(emails, parentPost.getSpace(), subject, compileEmailTemplate(model));
			}
		}

		if (isReplyNotificationAllowed() && !isMod(replyAuthor)) {
			createReportCopyOfNotificiation(name, postURL, subject, body, awaitingApproval);
		}

		if (awaitingApproval) {
			Report rep = new Report();
			rep.setName(parentPost.getTitle());
			rep.setContent(Utils.abbreviate(Utils.markdownToHtml(reply.getBody()), 2000));
			rep.setParentid(reply.getId());
			rep.setDescription(lang.get("reports.awaitingapproval") + (reply.isSpam() ? " [spam]" : ""));
			rep.setSubType(reply.isSpam() ? Report.ReportType.SPAM : Report.ReportType.OTHER);
			rep.setLink(parentPost.getPostLink(false, false) + "#post-" + reply.getId());
			rep.setAuthorName(replyAuthor.getName());
			rep.addProperty(lang.get("spaces.title"), getSpaceName(reply.getSpace()));
			rep.create();
		}
	}

	public void sendCommentNotifications(Post parentPost, Comment comment, Profile commentAuthor, HttpServletRequest req) {
		// send email notification to author of post except when the comment is by the same person
		if (parentPost != null && comment != null) {
			parentPost.setAuthor(pc.read(Profile.id(parentPost.getCreatorid()))); // parent author is not current user (authUser)
			Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(comment, false));
			payload.put("parent", parentPost);
			payload.put("author", commentAuthor);
			triggerHookEvent("comment.create", payload);
			// get the last 5-6 commentators who want to be notified - https://github.com/Erudika/scoold/issues/201
			Pager p = new Pager(1, Config._TIMESTAMP, false, 5);
			boolean isCommentatorThePostAuthor = StringUtils.equals(parentPost.getCreatorid(), comment.getCreatorid());
			Set<String> last5ids = pc.findChildren(parentPost, Utils.type(Comment.class),
					"!(" + Config._CREATORID + ":\"" + comment.getCreatorid() + "\")", p).
					stream().map(c -> c.getCreatorid()).distinct().collect(Collectors.toSet());
			if (!isCommentatorThePostAuthor && !last5ids.contains(parentPost.getCreatorid())) {
				last5ids = new HashSet<>(last5ids);
				last5ids.add(parentPost.getCreatorid());
			}
			Map<String, String> lang = getLang(req);

			if (isCommentNotificationAllowed()) {
				final List<Profile> last5 = pc.readAll(new ArrayList<>(last5ids));
				List<Profile> last5commentators = last5.stream().filter(u -> u.getCommentEmailsEnabled()).collect(Collectors.toList());
				Map<String, Object> model = new HashMap<String, Object>();
				String name = commentAuthor.getName();
				String body = Utils.markdownToHtml(comment.getComment());
				String pic = Utils.formatMessage("<img src='{0}' width='25'>",
						escapeHtmlAttribute(avatarRepository.getLink(commentAuthor, AvatarFormat.Square25)));
				String postURL = CONF.serverUrl() + parentPost.getPostLink(false, false) + "?commentid=" + comment.getId();
				String subject = Utils.formatMessage(lang.get("notification.comment.subject"), name, parentPost.getTitle());
				model.put("subject", escapeHtml(subject));
				model.put("logourl", getSmallLogoUrl());
				model.put("heading", Utils.formatMessage(lang.get("notification.comment.heading"),
						Utils.formatMessage("<a href='{0}'>{1}</a>", postURL, escapeHtml(parentPost.getTitle()))));
				model.put("body", Utils.formatMessage("<h2>{0} {1}:</h2><div class='panel'>{2}</div>", pic, escapeHtml(name), body));

				List<String> emails = pc.readAll(last5commentators.stream().map(u -> u.getCreatorid()).collect(Collectors.toList())).
						stream().map(author -> ((User) author).getEmail()).collect(Collectors.toList());

				emailer.sendEmail(emails, subject, compileEmailTemplate(model));
				createReportCopyOfNotificiation(name, postURL, subject, body, false);
			}
		}
	}

	public void deleteReportsAfterModAction(Post parent) {
		if (parent != null) {
			List<String> toDelete = new LinkedList<>();
			pc.readEverything(pager -> {
				List<ParaObject> objects = pc.getChildren(parent, Utils.type(Report.class), pager);
				toDelete.addAll(objects.stream().map(r -> r.getId()).collect(Collectors.toList()));
				return objects;
			});
			pc.deleteAll(toDelete);
		}
	}

	private void createReportCopyOfNotificiation(String author, String url, String subject, String template, boolean awaitingApproval) {
		if (CONF.notificationsAsReportsEnabled() && !awaitingApproval) {
			Report rep = new Report();
			rep.setContent(template);
			rep.setDescription(subject);
			rep.setSubType(Report.ReportType.OTHER);
			rep.setLink(url);
			rep.setAuthorName(author);
			rep.create();
		}
	}

	private String escapeHtmlAttribute(String value) {
		return StringUtils.trimToEmpty(value)
				.replaceAll("'", "%27")
				.replaceAll("\"", "%22")
				.replaceAll("\\\\", "");
	}

	private String escapeHtml(String value) {
		return StringEscapeUtils.escapeHtml4(value);
	}

	public Profile getAuthUser(HttpServletRequest req) {
		return (Profile) req.getAttribute(AUTH_USER_ATTRIBUTE);
	}

	public boolean isAuthenticated(HttpServletRequest req) {
		return getAuthUser(req) != null;
	}

	public boolean isFeedbackEnabled() {
		return CONF.feedbackEnabled();
	}

	public boolean isNearMeFeatureEnabled() {
		return CONF.postsNearMeEnabled();
	}

	public boolean isDefaultSpacePublic() {
		return CONF.isDefaultSpacePublic();
	}

	public boolean isWebhooksEnabled() {
		return CONF.webhooksEnabled();
	}

	public boolean isAnonymityEnabled() {
		return CONF.profileAnonimityEnabled();
	}

	public boolean isApiEnabled() {
		return CONF.apiEnabled();
	}

	public boolean isFooterLinksEnabled() {
		return CONF.footerLinksEnabled();
	}

	public boolean isNotificationsAllowed() {
		return CONF.notificationEmailsAllowed();
	}

	public boolean isNewPostNotificationAllowed() {
		return isNotificationsAllowed() && CONF.emailsForNewPostsAllowed();
	}

	public boolean isFavTagsNotificationAllowed() {
		return isNotificationsAllowed() && CONF.emailsForFavtagsAllowed();
	}

	public boolean isReplyNotificationAllowed() {
		return isNotificationsAllowed() && CONF.emailsForRepliesAllowed();
	}

	public boolean isCommentNotificationAllowed() {
		return isNotificationsAllowed() && CONF.emailsForCommentsAllowed();
	}

	public boolean isDarkModeEnabled() {
		return CONF.darkModeEnabled();
	}

	public boolean isSlackAuthEnabled() {
		return CONF.slackAuthEnabled();
	}

	public boolean isMicrosoftAuthEnabled() {
		return CONF.teamsAuthEnabled();
	}

	public static boolean isGravatarEnabled() {
		return CONF.gravatarsEnabled();
	}

	public static String gravatarPattern() {
		return CONF.gravatarsPattern();
	}

	public static String getDefaultAvatar() {
		return CONF.imagesLink() + "/anon.svg";
	}

	public static boolean isAvatarUploadsEnabled() {
		return isImgurAvatarRepositoryEnabled() || isCloudinaryAvatarRepositoryEnabled();
	}

	public static boolean isImgurAvatarRepositoryEnabled() {
		return !StringUtils.isBlank(CONF.imgurClientId()) && "imgur".equalsIgnoreCase(CONF.avatarRepository());
	}

	public static boolean isCloudinaryAvatarRepositoryEnabled() {
		return !StringUtils.isBlank(CONF.cloudinaryUrl()) && "cloudinary".equalsIgnoreCase(CONF.avatarRepository());
	}

	public String getFooterHTML() {
		return CONF.footerHtml();
	}

	public boolean isNavbarLink1Enabled() {
		return !StringUtils.isBlank(getNavbarLink1URL());
	}

	public String getNavbarLink1URL() {
		return CONF.navbarCustomLink1Url();
	}

	public String getNavbarLink1Text() {
		return CONF.navbarCustomLink1Text();
	}

	public boolean isNavbarLink2Enabled() {
		return !StringUtils.isBlank(getNavbarLink2URL());
	}

	public String getNavbarLink2URL() {
		return CONF.navbarCustomLink2Url();
	}

	public String getNavbarLink2Text() {
		return CONF.navbarCustomLink2Text();
	}

	public boolean isNavbarMenuLink1Enabled() {
		return !StringUtils.isBlank(getNavbarMenuLink1URL());
	}

	public String getNavbarMenuLink1URL() {
		return CONF.navbarCustomMenuLink1Url();
	}

	public String getNavbarMenuLink1Text() {
		return CONF.navbarCustomMenuLink1Text();
	}

	public boolean isNavbarMenuLink2Enabled() {
		return !StringUtils.isBlank(getNavbarMenuLink2URL());
	}

	public String getNavbarMenuLink2URL() {
		return CONF.navbarCustomMenuLink2Url();
	}

	public String getNavbarMenuLink2Text() {
		return CONF.navbarCustomMenuLink2Text();
	}

	public String getNavbarLink1Target() {
		return CONF.navbarCustomLink1Target();
	}

	public String getNavbarLink2Target() {
		return CONF.navbarCustomLink2Target();
	}

	public String getNavbarMenuLink1Target() {
		return CONF.navbarCustomMenuLink1Target();
	}

	public String getNavbarMenuLink2Target() {
		return CONF.navbarCustomMenuLink2Target();
	}

	public boolean alwaysHideCommentForms() {
		return CONF.alwaysHideCommentForms();
	}

	public Set<String> getCoreScooldTypes() {
		return CoreUtils.getCoreTypes();
	}

	public Set<String> getCustomHookEvents() {
		return Set.copyOf(HOOK_EVENTS);
	}

	public Pager getPager(String pageParamName, HttpServletRequest req) {
		return pagerFromParams(pageParamName, req);
	}

	public Pager pagerFromParams(HttpServletRequest req) {
		return pagerFromParams("page", req);
	}

	public Pager pagerFromParams(String pageParamName, HttpServletRequest req) {
		Pager p = new Pager(CONF.maxItemsPerPage());
		p.setPage(Math.min(NumberUtils.toLong(req.getParameter(pageParamName), 1), CONF.maxPages()));
		String paramSuffix = StringUtils.substringAfter(pageParamName, "page");
		String lastKey = Optional.ofNullable(req.getParameter("lastKey")).orElse(req.getParameter("lastKey" + paramSuffix));
		String sort = Optional.ofNullable(req.getParameter("sortby")).orElse(req.getParameter("sortby" + paramSuffix));
		String desc = Optional.ofNullable(req.getParameter("desc")).orElse(req.getParameter("desc" + paramSuffix));
		String limit = Optional.ofNullable(req.getParameter("limit")).orElse(req.getParameter("limit" + paramSuffix));
		if (!StringUtils.isBlank(desc)) {
			p.setDesc(Boolean.parseBoolean(desc));
		}
		if (!StringUtils.isBlank(lastKey)) {
			p.setLastKey(lastKey);
		}
		if (!StringUtils.isBlank(sort)) {
			p.setSortby(sort);
		}
		if (!StringUtils.isBlank(limit)) {
			p.setLimit(NumberUtils.toInt(limit, CONF.maxItemsPerPage()));
		}
		return p;
	}

	public String getLanguageCode(HttpServletRequest req) {
		String langCodeFromConfig = CONF.defaultLanguageCode();
		String cookieLoc = getCookieValue(req, CONF.localeCookie());
		Locale fromReq = (req == null) ? Locale.getDefault() : req.getLocale();
		Locale requestLocale = langutils.getProperLocale(fromReq.toString());
		return (cookieLoc != null) ? cookieLoc : (StringUtils.isBlank(langCodeFromConfig) ?
				requestLocale.getLanguage() : langutils.getProperLocale(langCodeFromConfig).getLanguage());
	}

	public Locale getCurrentLocale(String langname) {
		Locale currentLocale = langutils.getProperLocale(langname);
		if (currentLocale == null) {
			currentLocale = langutils.getProperLocale(langutils.getDefaultLanguageCode());
		}
		return currentLocale;
	}

	public Map<String, String> getLang(HttpServletRequest req) {
		return getLang(getCurrentLocale(getLanguageCode(req)));
	}

	public Map<String, String> getLang(Locale currentLocale) {
		Map<String, String> lang = langutils.readLanguage(currentLocale.toString());
		if (lang == null || lang.isEmpty()) {
			lang = langutils.getDefaultLanguage();
		}
		return lang;
	}

	public boolean isLanguageRTL(String langCode) {
		return StringUtils.equalsAnyIgnoreCase(langCode, "ar", "he", "dv", "iw", "fa", "ps", "sd", "ug", "ur", "yi");
	}

	public void getProfiles(List<? extends ParaObject> objects) {
		if (objects == null || objects.isEmpty()) {
			return;
		}
		Map<String, String> authorids = new HashMap<String, String>(objects.size());
		Map<String, Profile> authors = new HashMap<String, Profile>(objects.size());
		for (ParaObject obj : objects) {
			if (obj.getCreatorid() != null) {
				authorids.put(obj.getId(), obj.getCreatorid());
			}
		}
		List<String> ids = new ArrayList<String>(new HashSet<String>(authorids.values()));
		if (ids.isEmpty()) {
			return;
		}
		// read all post authors in batch
		for (ParaObject author : pc.readAll(ids)) {
			authors.put(author.getId(), (Profile) author);
		}
		// add system profile
		authors.put(API_USER.getId(), API_USER);
		// set author object for each post
		for (ParaObject obj : objects) {
			if (obj instanceof Post) {
				((Post) obj).setAuthor(authors.get(authorids.get(obj.getId())));
			} else if (obj instanceof Revision) {
				((Revision) obj).setAuthor(authors.get(authorids.get(obj.getId())));
			}
		}
	}

	//get the comments for each answer and the question
	public void getComments(List<Post> allPosts) {
		Map<String, List<Comment>> allComments = new HashMap<String, List<Comment>>();
		List<String> allCommentIds = new ArrayList<String>();
		List<Post> forUpdate = new ArrayList<Post>(allPosts.size());
		// get the comment ids of the first 5 comments for each post
		for (Post post : allPosts) {
			// not set => read comments if any and embed ids in post object
			if (post.getCommentIds() == null) {
				forUpdate.add(reloadFirstPageOfComments(post));
				allComments.put(post.getId(), post.getComments());
			} else {
				// ids are set => add them to list for bulk read
				allCommentIds.addAll(post.getCommentIds());
			}
		}
		if (!allCommentIds.isEmpty()) {
			// read all comments for all posts on page in bulk
			for (ParaObject comment : pc.readAll(allCommentIds)) {
				List<Comment> postComments = allComments.get(comment.getParentid());
				if (postComments == null) {
					allComments.put(comment.getParentid(), new ArrayList<Comment>());
				}
				allComments.get(comment.getParentid()).add((Comment) comment);
			}
		}
		// embed comments in each post for use within the view
		for (Post post : allPosts) {
			List<Comment> cl = allComments.get(post.getId());
			long clSize = (cl == null) ? 0 : cl.size();
			if (post.getCommentIds().size() != clSize) {
				forUpdate.add(reloadFirstPageOfComments(post));
				clSize = post.getComments().size();
			} else {
				post.setComments(cl);
				if (clSize == post.getItemcount().getLimit() && pc.getCount(Utils.type(Comment.class),
						Collections.singletonMap("parentid", post.getId())) > clSize) {
					clSize++; // hack to show the "more" button
				}
			}
			post.getItemcount().setCount(clSize);
		}
		if (!forUpdate.isEmpty()) {
			pc.updateAll(allPosts);
		}
	}

	public void getLinkedComment(Post showPost, HttpServletRequest req) {
		if (showPost != null && req.getParameter("commentid") != null) {
			Comment c = pc.read(req.getParameter("commentid"));
			if (c != null) {
				if (showPost.getComments() == null) {
					showPost.setComments(List.of(c));
					showPost.getItemcount().setCount(1);
				} else {
					Set<Comment> comments = new LinkedHashSet<>(showPost.getComments());
					comments.add(c);
					showPost.setComments(List.of(comments.toArray(Comment[]::new)));
				}
			}
		}
	}

	public void getVotes(List<Post> allPosts, Profile authUser) {
		if (authUser == null) {
			return;
		}
		Map<String, Vote> allVotes = new HashMap<>();
		List<String> allVoteIds = new ArrayList<String>();
		for (Post post : allPosts) {
			allVoteIds.add(new Vote(authUser.getId(), post.getId(), Votable.VoteValue.UP).getId());
		}
		if (!allVoteIds.isEmpty()) {
			for (ParaObject vote : pc.readAll(allVoteIds)) {
				allVotes.put(((Vote) vote).getParentid(), (Vote) vote);
			}
		}
		for (Post post : allPosts) {
			post.setVote(allVotes.get(post.getId()));
		}
	}

	public Post reloadFirstPageOfComments(Post post) {
		List<Comment> commentz = pc.getChildren(post, Utils.type(Comment.class), post.getItemcount());
		ArrayList<String> ids = new ArrayList<String>(commentz.size());
		for (Comment comment : commentz) {
			ids.add(comment.getId());
		}
		post.setCommentIds(ids);
		post.setComments(commentz);
		return post;
	}

	public void updateViewCount(Post showPost, HttpServletRequest req, HttpServletResponse res) {
		//do not count views from author
		if (showPost != null && !isMine(showPost, getAuthUser(req))) {
			String postviews = StringUtils.trimToEmpty(HttpUtils.getStateParam("postviews", req));
			if (!StringUtils.contains(postviews, showPost.getId())) {
				long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
				showPost.setViewcount(views + 1); //increment count
				HttpUtils.setStateParam("postviews", (postviews.isEmpty() ? "" : postviews + ".") + showPost.getId(),
						req, res);
				pc.update(showPost);
			}
		}
	}

	public List<Post> getSimilarPosts(Post showPost, Pager pager) {
		List<Post> similarquestions = Collections.emptyList();
		if (!showPost.isReply()) {
			String likeTxt = Utils.stripAndTrim((showPost.getTitle() + " " + showPost.getBody()));
			if (likeTxt.length() > 1000) {
				// read object on the server to prevent "URI too long" errors
				similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
						new String[]{"properties.title", "properties.body", "properties.tags"},
						"id:" + showPost.getId(), pager);
			} else if (!StringUtils.isBlank(likeTxt)) {
				similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
						new String[]{"properties.title", "properties.body", "properties.tags"}, likeTxt, pager);
			}
		}
		return similarquestions;
	}

	public String getFirstLinkInPost(String postBody) {
		postBody = StringUtils.trimToEmpty(postBody);
		Pattern p = Pattern.compile("^!?\\[.*\\]\\((.+)\\)");
		Matcher m = p.matcher(postBody);

		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	public boolean param(HttpServletRequest req, String param) {
		return req.getParameter(param) != null;
	}

	public boolean isAjaxRequest(HttpServletRequest req) {
		return req.getHeader("X-Requested-With") != null || req.getParameter("X-Requested-With") != null;
	}

	public boolean isApiRequest(HttpServletRequest req) {
		return req.getServletPath().startsWith("/api/") || req.getServletPath().equals("/api");
	}

	public boolean isAdmin(Profile authUser) {
		return authUser != null &&
				(User.Groups.ADMINS.toString().equals(authUser.getGroups()) && authUser.getEditorRoleEnabled());
	}

	public boolean isMod(Profile authUser) {
		if (authUser == null || !authUser.getEditorRoleEnabled()) {
			return false;
		}
		if (ScooldUtils.getConfig().modsAccessAllSpaces()) {
			return isAdmin(authUser) || User.Groups.MODS.toString().equals(authUser.getGroups());
		} else {
			return isAdmin(authUser) || authUser.isModInCurrentSpace();
		}
	}

	public boolean isModAnywhere(Profile authUser) {
		if (authUser == null) {
			return false;
		}
		return ScooldUtils.getConfig().modsAccessAllSpaces() ? isMod(authUser) : !authUser.getModspaces().isEmpty();
	}

	public boolean isRecognizedAsAdmin(User u) {
		return u.isAdmin() || ADMINS.contains(u.getIdentifier()) ||
				ADMINS.stream().filter(s -> s.equalsIgnoreCase(u.getEmail())).findAny().isPresent();
	}

	public boolean canComment(Profile authUser, HttpServletRequest req) {
		return isAuthenticated(req) && ((authUser.hasBadge(ENTHUSIAST) || CONF.newUsersCanComment() || isMod(authUser)));
	}

	public boolean postsNeedApproval(HttpServletRequest req) {
		Profile authUser = getAuthUser(req);
		String spaceId = getSpaceId(getSpaceIdFromCookie(authUser, req));
		Sysprop s = getAllSpaces().parallelStream().filter(ss -> ss.getId().equals(spaceId)).findFirst().orElse(null);
		return (s == null) ? CONF.postsNeedApproval() :
				(boolean) s.getProperties().getOrDefault("posts_need_approval", CONF.postsNeedApproval());
	}

	public boolean userNeedsApproval(Profile authUser) {
		return (authUser == null || authUser.getVotes() < CONF.postsReputationThreshold()) && !isMod(authUser);
	}

	public String getWelcomeMessage(Profile authUser) {
		return authUser == null ? CONF.welcomeMessage().replaceAll("'", "&apos;") : "";
	}

	public String getWelcomeMessageOnLogin(Profile authUser) {
		if (authUser == null) {
			return "";
		}
		String welcomeMsgOnlogin = CONF.welcomeMessageOnLogin();
		if (StringUtils.contains(welcomeMsgOnlogin, "{{")) {
			welcomeMsgOnlogin = Utils.compileMustache(Collections.singletonMap("user",
					ParaObjectUtils.getAnnotatedFields(authUser, false)), welcomeMsgOnlogin);
		}
		return welcomeMsgOnlogin.replaceAll("'", "&apos;");
	}

	public String getWelcomeMessagePreLogin(Profile authUser, HttpServletRequest req) {
		if (StringUtils.startsWithIgnoreCase(req.getServletPath(), SIGNINLINK)) {
			return authUser == null ? CONF.welcomeMessagePreLogin().replaceAll("'", "&apos;") : "";
		}
		return "";
	}

	public boolean isDefaultSpace(String space) {
		return DEFAULT_SPACE.equalsIgnoreCase(getSpaceId(space));
	}

	public boolean isDefaultSpace(Sysprop space) {
		return space != null && isDefaultSpace(space.getId());
	}

	public String getDefaultSpace() {
		return DEFAULT_SPACE;
	}

	public boolean isAllSpaces(String space) {
		return ALL_MY_SPACES.equalsIgnoreCase(getSpaceId(space));
	}

	public Set<Sysprop> getAllSpaces() {
		return getAllSpacesAdmin();
	}

	public Set<Sysprop> getAllSpacesAdmin() {
		if (Utils.timestamp() - lastSpacesCountTimestamp > TimeUnit.SECONDS.toMillis(30)) {
			lastSpacesCountTimestamp = Utils.timestamp();
			spacesCount = pc.getCount("scooldspace").intValue();
		}
		if (allSpaces == null || spacesCount != allSpaces.size()) { // caching issue on >1 nodes
			allSpaces = new LinkedHashSet<>(pc.findQuery("scooldspace", "*", new Pager(Config.DEFAULT_LIMIT)));
		}
		return allSpaces.stream().sorted((s1, s2) -> getSpaceName(s1.getName()).compareToIgnoreCase(getSpaceName(s2.getName()))).
				collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public void addSpaceToCachedList(Sysprop space) {
		if (space != null) {
			if (allSpaces == null || allSpaces.isEmpty()) {
				getAllSpacesAdmin();
			}
			allSpaces.add(space);
		}
	}

	public void removeSpaceFromCachedList(Sysprop space) {
		if (space != null) {
			if (allSpaces == null || allSpaces.isEmpty()) {
				getAllSpacesAdmin();
			}
			allSpaces.remove(space);
		}
	}

	public boolean canAccessSpace(Profile authUser, String targetSpaceId) {
		if (authUser == null) {
			return isDefaultSpacePublic() && isDefaultSpace(targetSpaceId);
		}
		if ((isMod(authUser) && CONF.modsAccessAllSpaces()) || isAllSpaces(targetSpaceId)) {
			return true;
		}
		if (StringUtils.isBlank(targetSpaceId) || targetSpaceId.length() < 2) {
			return false;
		}
		// this is confusing - let admins control who is in the default space
		//if (isDefaultSpace(targetSpaceId)) {
		//	// can user access the default space (blank)
		//	return isDefaultSpacePublic() || isMod(authUser) || !authUser.hasSpaces();
		//}
		boolean isMemberOfSpace = false;
		for (String space : authUser.getSpaces()) {
			String spaceId = getSpaceId(targetSpaceId);
			if (StringUtils.startsWithIgnoreCase(space, spaceId + Para.getConfig().separator()) || space.equalsIgnoreCase(spaceId)) {
				isMemberOfSpace = true;
				break;
			}
		}
		return isMemberOfSpace;
	}

	private boolean isIgnoredSpaceForNotifications(Profile profile, String space) {
		return profile != null && !profile.getFavspaces().isEmpty() && !profile.getFavspaces().contains(getSpaceId(space));
	}

	public String getSpaceIdFromCookie(Profile authUser, HttpServletRequest req) {
		if (isAdmin(authUser) && req.getParameter("space") != null) {
			Sysprop s = pc.read(getSpaceId(req.getParameter("space"))); // API override
			if (s != null) {
				return s.getId() + Para.getConfig().separator() + s.getName();
			}
		}
		String spaceAttr = (String) req.getAttribute(CONF.spaceCookie());
		String spaceValue = StringUtils.isBlank(spaceAttr) ? Utils.base64dec(getCookieValue(req, CONF.spaceCookie())) : spaceAttr;
		String space = getValidSpaceId(authUser, spaceValue);
		return verifyExistingSpace(authUser, space);
	}

	public void storeSpaceIdInCookie(String space, HttpServletRequest req, HttpServletResponse res) {
		// directly set the space on the requests, overriding the cookie value
		// used for setting the space from a direct URL to a particular space
		req.setAttribute(CONF.spaceCookie(), space);
		HttpUtils.setRawCookie(CONF.spaceCookie(), Utils.base64encURL(space.getBytes()),
				req, res, "Lax", StringUtils.isBlank(space) ? 0 : 365 * 24 * 60 * 60);
	}

	public String verifyExistingSpace(Profile authUser, String space) {
		if (!isDefaultSpace(space) && !isAllSpaces(space)) {
			Sysprop s = pc.read(getSpaceId(space));
			if (s == null) {
				if (authUser != null) {
					authUser.removeSpace(space);
					pc.update(authUser);
				}
				return DEFAULT_SPACE;
			} else {
				return s.getId() + Para.getConfig().separator() + s.getName(); // updates current space name in case it was renamed
			}
		}
		return space;
	}

	public String getValidSpaceIdExcludingAll(Profile authUser, String space, HttpServletRequest req) {
		String s = StringUtils.isBlank(space) ? getSpaceIdFromCookie(authUser, req) : space;
		return isAllSpaces(s) ? DEFAULT_SPACE : s;
	}

	private String getValidSpaceId(Profile authUser, String space) {
		if (authUser == null) {
			return DEFAULT_SPACE;
		}
		String defaultSpace = authUser.hasSpaces() ? ALL_MY_SPACES : DEFAULT_SPACE;
		String s = canAccessSpace(authUser, space) ? space : defaultSpace;
		return StringUtils.isBlank(s) ? DEFAULT_SPACE : s;
	}

	public String getSpaceName(String space) {
		if (DEFAULT_SPACE.equalsIgnoreCase(space)) {
			return "";
		}
		return RegExUtils.replaceAll(space, "^scooldspace:[^:]+:", "");
	}

	public String getSpaceId(String space) {
		if (StringUtils.isBlank(space) || "default".equalsIgnoreCase(space)) {
			return DEFAULT_SPACE;
		}
		String s = StringUtils.contains(space, Para.getConfig().separator()) ?
				StringUtils.substring(space, 0, space.lastIndexOf(Para.getConfig().separator())) : "scooldspace:" + space;
		return "scooldspace".equals(s) ? space : s;
	}

	public String getSpaceFilteredQuery(Profile authUser, String currentSpace) {
		return canAccessSpace(authUser, currentSpace) ? getSpaceFilter(authUser, currentSpace) : "";
	}

	public String getSpaceFilteredQuery(HttpServletRequest req) {
		Profile authUser = getAuthUser(req);
		String currentSpace = getSpaceIdFromCookie(authUser, req);
		return getSpaceFilteredQuery(authUser, currentSpace);
	}

	public String getSpaceFilteredQuery(HttpServletRequest req, boolean isSpaceFiltered, String spaceFilter, String defaultQuery) {
		Profile authUser = getAuthUser(req);
		String currentSpace = getSpaceIdFromCookie(authUser, req);
		if (isSpaceFiltered) {
			return StringUtils.isBlank(spaceFilter) ? getSpaceFilter(authUser, currentSpace) : spaceFilter;
		}
		return canAccessSpace(authUser, currentSpace) ? defaultQuery : "";
	}

	public String getSpaceFilter(Profile authUser, String spaceId) {
		if (isAllSpaces(spaceId)) {
			if (isMod(authUser) && CONF.modsAccessAllSpaces()) {
				return "*";
			} else if (authUser != null && authUser.hasSpaces()) {
				return "(" + authUser.getSpaces().stream().map(s -> "properties.space:\"" + s + "\"").
						collect(Collectors.joining(" OR ")) + ")";
			} else {
				return "properties.space:\"" + DEFAULT_SPACE + "\"";
			}
//		} else if (isDefaultSpace(spaceId) && isMod(authUser)) { // DO NOT MODIFY!
//			return "*";
		} else {
			return "properties.space:\"" + spaceId + "\"";
		}
	}

	public Sysprop buildSpaceObject(String space) {
		String spaceId, spaceName;
		String col = Para.getConfig().separator();
		if (space.startsWith(getSpaceId(space))) {
			spaceId = StringUtils.substringBefore(StringUtils.substringAfter(space, col), col);
			spaceName = StringUtils.substringAfterLast(space, col);
		} else {
			spaceId = space;
			spaceName = Utils.abbreviate(space, 255).replaceAll(col, "");
		}
		Sysprop s = new Sysprop();
		s.setType("scooldspace");
		s.setId(getSpaceId(Utils.noSpaces(Utils.stripAndTrim(spaceId, " "), "-")));
		s.setName(isDefaultSpace(space) ? "default" : spaceName);
		return s;
	}

	public boolean isAutoAssignedSpace(Sysprop space) {
		return space != null && (isAutoAssignedSpaceInConfig(space) ||
				Optional.ofNullable(space.getTags()).orElse(List.of()).contains("assign-to-all"));
	}

	public boolean isAutoAssignedSpaceInConfig(Sysprop space) {
		return space != null && (getAutoAssignedSpacesFromConfig().contains(space.getName()) ||
				getAutoAssignedSpacesFromConfig().stream().map(s -> buildSpaceObject(s).getId()).
						anyMatch(i -> i.equalsIgnoreCase(space.getId())));
	}

	public Set<String> getAutoAssignedSpacesFromConfig() {
		if (autoAssignedSpacesFromConfig == null) {
			autoAssignedSpacesFromConfig = Set.of(ScooldUtils.getConfig().autoAssignSpaces().split("\\s*,\\s*"));
		}
		return autoAssignedSpacesFromConfig;
	}

	public String[] getAllAutoAssignedSpaces() {
		Set<String> allAutoAssignedSpaces = new LinkedHashSet<>();
		allAutoAssignedSpaces.addAll(getAllSpaces().parallelStream().
				filter(this::isAutoAssignedSpace).
				filter(Predicate.not(this::isDefaultSpace)).
				map(s -> s.getId() + Para.getConfig().separator() + s.getName()).collect(Collectors.toSet()));
		allAutoAssignedSpaces.addAll(getAutoAssignedSpacesFromConfig());
		return allAutoAssignedSpaces.toArray(String[]::new);
	}

	public boolean assignSpacesToUser(Profile authUser, String... spaces) {
		if (spaces != null && spaces.length > 0) {
			//DO: CHECK IF SPACES HAVE CHANGED FIRST! NO CHANGE - NO OP
			Map<String, Sysprop> spaceObjectsMap = new HashMap<>(spaces.length);
			for (String space : spaces) {
				Sysprop s = buildSpaceObject(space);
				spaceObjectsMap.put(s.getId(), s);
			}
			List<Sysprop> spacez = pc.readAll(new ArrayList<>(spaceObjectsMap.keySet()));
			Set<String> assignedSpaces = new HashSet<>(spacez.size());
			for (Sysprop space : spacez) {
				assignedSpaces.add(space.getId() + Para.getConfig().separator() + space.getName());
				spaceObjectsMap.remove(space.getId());
			}
			if (CONF.resetSpacesOnNewAssignment(authUser.getUser().isOAuth2User()
					|| authUser.getUser().isLDAPUser() || authUser.getUser().isSAMLUser())) {
				authUser.setSpaces(assignedSpaces);
			} else {
				authUser.getSpaces().addAll(assignedSpaces);
			}
			if (!spaceObjectsMap.isEmpty()) {
				// create the remaining spaces which were missing
				ArrayList<Sysprop> missingSpaces = new ArrayList<>(spaceObjectsMap.size());
				for (Sysprop missingSpace : spaceObjectsMap.values()) {
					authUser.getSpaces().add(missingSpace.getId() + Para.getConfig().separator() + missingSpace.getName());
					missingSpaces.add(missingSpace);
					getAllSpaces().add(missingSpace); // if we don't add it admins won't see the new space in the list
				}
				pc.createAll(missingSpaces);
				return true;
			}
			// Please, consider buying Scoold Pro which doesn't have this limitation.
			if (authUser.getSpaces().size() > MAX_SPACES) {
				authUser.setSpaces(authUser.getSpaces().stream().limit(MAX_SPACES).collect(Collectors.toSet()));
			}
		}
		return false;
	}

	public void assingSpaceToAllUsers(Sysprop space) {
		if (space == null) {
			return;
		}
		Para.asyncExecute(() -> {
			pc.updateAllPartially((toUpdate, pager) -> {
				List<Profile> profiles = pc.findQuery(Utils.type(Profile.class), "*", pager);
				profiles.stream().forEach(p -> {
					Map<String, Object> profile = new HashMap<>();
					profile.put(Config._ID, p.getId());
					p.getSpaces().add(space.getId() + Para.getConfig().separator() + space.getName());
					profile.put("spaces", p.getSpaces());
					toUpdate.add(profile);
				});
				return profiles;
			});
		});
	}

	public String sanitizeQueryString(String query, HttpServletRequest req) {
		String qf = getSpaceFilteredQuery(req);
		String defaultQuery = "*";
		String q = StringUtils.trimToEmpty(query);
		if (qf.isEmpty() || qf.length() > 1) {
			q = q.replaceAll("[\\?<>]", "").trim();
			q = q.replaceAll("$[\\*]*", "");
			q = RegExUtils.removeAll(q, "AND");
			q = RegExUtils.removeAll(q, "OR");
			q = RegExUtils.removeAll(q, "NOT");
			q = q.trim();
			defaultQuery = "";
		}
		if (qf.isEmpty()) {
			return defaultQuery;
		} else if ("*".equals(qf)) {
			return q;
		} else if ("*".equals(q)) {
			return qf;
		} else {
			if (q.isEmpty()) {
				return qf;
			} else {
				return qf + " AND " + q;
			}
		}
	}

	public String getUsersSearchQuery(String qs, String spaceFilter) {
		qs = Utils.stripAndTrim(qs).toLowerCase();
		if (!StringUtils.isBlank(qs)) {
			String wildcardLower = qs.matches("[\\p{IsAlphabetic}]*") ? qs + "*" : qs;
			String wildcardUpper = StringUtils.capitalize(wildcardLower);
			String template = "(name:({1}) OR name:({2} OR properties.originalName:{1} OR properties.originalName:{2} OR {3}) "
					+ "OR properties.location:({0}) OR properties.aboutme:({0}) OR properties.groups:({0}))";
			qs = (StringUtils.isBlank(spaceFilter) ? "" : spaceFilter + " AND ") +
					Utils.formatMessage(template, qs, StringUtils.capitalize(qs), wildcardLower, wildcardUpper);
		} else {
			qs = StringUtils.isBlank(spaceFilter) ? "*" : spaceFilter;
		}
		return qs;
	}

	public List<Post> fullQuestionsSearch(String query, Pager... pager) {
		String typeFilter = Config._TYPE + ":(" + String.join(" OR ",
						Utils.type(Question.class), Utils.type(Reply.class), Utils.type(Comment.class)) + ")";
		String qs = StringUtils.isBlank(query) || query.startsWith("*") ? typeFilter : query + " AND " + typeFilter;
		List<ParaObject> mixedResults = pc.findQuery("", qs, pager);
		Predicate<ParaObject> isQuestion =  obj -> obj.getType().equals(Utils.type(Question.class));

		Map<String, ParaObject> idsToQuestions = new HashMap<>(mixedResults.stream().filter(isQuestion).
				collect(Collectors.toMap(q -> q.getId(), q -> q)));
		Set<String> toRead = new LinkedHashSet<>();
		mixedResults.stream().filter(isQuestion.negate()).forEach(obj -> {
			if (!idsToQuestions.containsKey(obj.getParentid())) {
				toRead.add(obj.getParentid());
			}
		});
		// find all parent posts but this excludes parents of parents - i.e. won't work for comments in answers
		List<Post> parentPostsLevel1 = pc.readAll(new ArrayList<>(toRead));
		parentPostsLevel1.stream().filter(isQuestion).forEach(q -> idsToQuestions.put(q.getId(), q));

		toRead.clear();

		// read parents of parents if any
		parentPostsLevel1.stream().filter(isQuestion.negate()).forEach(obj -> {
			if (!idsToQuestions.containsKey(obj.getParentid())) {
				toRead.add(obj.getParentid());
			}
		});
		List<Post> parentPostsLevel2 = pc.readAll(new ArrayList<>(toRead));
		parentPostsLevel2.stream().forEach(q -> idsToQuestions.put(q.getId(), q));

		ArrayList<Post> results = new ArrayList<Post>(idsToQuestions.size());
		for (ParaObject result : idsToQuestions.values()) {
			if (result instanceof Post) {
				results.add((Post) result);
			}
		}
		return results;
	}

	public String getMacroCode(String key) {
		return WHITELISTED_MACROS.getOrDefault(key, "");
	}

	public boolean isMine(Post showPost, Profile authUser) {
		// author can edit, mods can edit & ppl with rep > 100 can edit
		return showPost != null && authUser != null ? authUser.getId().equals(showPost.getCreatorid()) : false;
	}

	public boolean canEdit(Post showPost, Profile authUser) {
		return authUser != null ? (authUser.hasBadge(TEACHER) || isMod(authUser) || isMine(showPost, authUser)) : false;
	}

	public boolean canDelete(Post showPost, Profile authUser) {
		return canDelete(showPost, authUser, null);
	}

	public boolean canDelete(Post showPost, Profile authUser, String approvedAnswerId) {
		if (authUser == null) {
			return false;
		}
		if (CONF.deleteProtectionEnabled()) {
			if (showPost.isReply()) {
				return isMine(showPost, authUser) && !StringUtils.equals(approvedAnswerId, showPost.getId());
			} else {
				return isMine(showPost, authUser) && showPost.getAnswercount() == 0;
			}
		}
		return isMine(showPost, authUser);
	}

	public boolean canApproveReply(Post showPost, Profile authUser) {
		switch (CONF.answersApprovedBy()) {
			case "admins":
				return isAdmin(authUser);
			case "moderators":
			case "mods":
				return isMod(authUser);
			default:
				return canEdit(showPost, authUser) && (isMine(showPost, authUser) || isMod(authUser));
		}
	}

	@SuppressWarnings("unchecked")
	public <P extends ParaObject> P populate(HttpServletRequest req, P pobj, String... paramName) {
		if (pobj == null || paramName == null) {
			return pobj;
		}
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		if (isApiRequest(req)) {
			try {
				data = (Map<String, Object>) req.getAttribute(REST_ENTITY_ATTRIBUTE);
				if (data == null) {
					data = ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
				}
			} catch (IOException ex) {
				logger.error(null, ex);
				data = Collections.emptyMap();
			}
		} else {
			for (String param : paramName) {
				String[] values;
				if (param.matches(".+?\\|.$")) {
					// convert comma-separated value to list of strings
					String cleanParam = param.substring(0, param.length() - 2);
					values = req.getParameterValues(cleanParam);
					String firstValue = (values != null && values.length > 0) ? values[0] : null;
					String separator = param.substring(param.length() - 1);
					if (!StringUtils.isBlank(firstValue)) {
						data.put(cleanParam, Arrays.asList(firstValue.split(separator)));
					}
				} else {
					values = req.getParameterValues(param);
					if (values != null && values.length > 0) {
						data.put(param, values.length > 1 ? Arrays.asList(values) :
								Arrays.asList(values).iterator().next());
					}
				}
			}
		}
		if (!data.isEmpty()) {
			ParaObjectUtils.setAnnotatedFields(pobj, data, null);
		}
		return pobj;
	}

	public <P extends ParaObject> Map<String, String> validate(P pobj) {
		HashMap<String, String> error = new HashMap<String, String>();
		if (pobj != null) {
			Set<ConstraintViolation<P>> errors = ValidationUtils.getValidator().validate(pobj);
			for (ConstraintViolation<P> err : errors) {
				error.put(err.getPropertyPath().toString(), err.getMessage());
			}
		}
		return error;
	}

	public Map<String, String> validateQuestionTags(Question q, Map<String, String> errors, HttpServletRequest req) {
		Set<String> tagz = Optional.ofNullable(q.getTags()).orElse(List.of()).stream().
				filter(t -> !StringUtils.isBlank(t)).distinct().collect(Collectors.toSet());
		long tagCount = tagz.size();
		if (!CONF.tagCreationAllowed() && !ScooldUtils.getInstance().isMod(q.getAuthor())) {
			q.setTags(pc.findByIds(tagz.stream().map(t -> new Tag(t).getId()).collect(Collectors.toList())).stream().
					map(tt -> ((Tag) tt).getTag()).collect(Collectors.toList()));
			tagCount = q.getTags().size();
		}
		if (CONF.minTagsPerPost() > tagCount) {
			errors.put(Config._TAGS, Utils.formatMessage(getLang(req).get("tags.toofew"), CONF.minTagsPerPost()));
		}
		return errors;
	}

	public String getFullAvatarURL(Profile profile, AvatarFormat format) {
		return avatarRepository.getLink(profile, format);
	}

	public void clearSession(HttpServletRequest req, HttpServletResponse res) {
		if (req != null) {
			String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
			if (!StringUtils.isBlank(jwt)) {
				if (CONF.oneSessionPerUser()) {
					logger.debug("Trying to revoke all user tokens for user...");
					ParaClient pcc = new ParaClient(CONF.paraAccessKey(), CONF.paraSecretKey());
					setParaEndpointAndApiPath(pcc);
					pcc.setAccessToken(jwt);
					pcc.revokeAllTokens();
					pcc.signOut();
				}
				HttpUtils.removeStateParam(CONF.authCookie(), req, res);
				HttpUtils.set2FACookie(null, null, req, res);
			}
			HttpUtils.removeStateParam("dark-mode", req, res);
		}
	}

	public boolean addBadgeOnce(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadge(authUser, b, condition && !authUser.hasBadge(b), false);
	}

	public boolean addBadgeOnceAndUpdate(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadgeAndUpdate(authUser, b, condition && authUser != null && !authUser.hasBadge(b));
	}

	public boolean addBadgeAndUpdate(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadge(authUser, b, condition, true);
	}

	public boolean addBadge(Profile user, Profile.Badge b, boolean condition, boolean update) {
		if (user != null && condition) {
			String newb = StringUtils.isBlank(user.getNewbadges()) ? "" : user.getNewbadges().concat(",");
			newb = newb.concat(b.toString());

			user.addBadge(b);
			user.setNewbadges(newb);
			if (update) {
				user.update();
				return true;
			}
		}
		return false;
	}

	public List<String> checkForBadges(Profile authUser, HttpServletRequest req) {
		List<String> badgelist = new ArrayList<String>();
		if (authUser != null && !isAjaxRequest(req)) {
			long oneYear = authUser.getTimestamp() + (365 * 24 * 60 * 60 * 1000);
			addBadgeOnce(authUser, Profile.Badge.ENTHUSIAST, authUser.getVotes() >= CONF.enthusiastIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.FRESHMAN, authUser.getVotes() >= CONF.freshmanIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.SCHOLAR, authUser.getVotes() >= CONF.scholarIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.TEACHER, authUser.getVotes() >= CONF.teacherIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.PROFESSOR, authUser.getVotes() >= CONF.professorIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.GEEK, authUser.getVotes() >= CONF.geekIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.SENIOR, (System.currentTimeMillis() - authUser.getTimestamp()) >= oneYear);

			if (!StringUtils.isBlank(authUser.getNewbadges())) {
				badgelist.addAll(Arrays.asList(authUser.getNewbadges().split(",")));
				authUser.setNewbadges(null);
				authUser.update();
			}
		}
		return badgelist;
	}

	private String loadEmailTemplate(String name) {
		return loadResource("emails/" + name + ".html");
	}

	public String loadResource(String filePath) {
		if (filePath == null) {
			return "";
		}
		if (FILE_CACHE.containsKey(filePath)) {
			return FILE_CACHE.get(filePath);
		}
		String template = "";
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(filePath)) {
			try (Scanner s = new Scanner(in).useDelimiter("\\A")) {
				template = s.hasNext() ? s.next() : "";
				if (!StringUtils.isBlank(template)) {
					FILE_CACHE.put(filePath, template);
				}
			}
		} catch (Exception ex) {
			logger.info("Couldn't load resource '{}'.", filePath);
		}
		return template;
	}

	public String compileEmailTemplate(Map<String, Object> model) {
		model.put("footerhtml", CONF.emailsFooterHtml());
		String fqdn = CONF.rewriteInboundLinksWithFQDN();
		if (!StringUtils.isBlank(fqdn)) {
			model.entrySet().stream().filter(e -> (e.getValue() instanceof String)).forEachOrdered(e -> {
				model.put(e.getKey(), StringUtils.replace((String) e.getValue(), CONF.serverUrl(), fqdn));
			});
		}
		return Utils.compileMustache(model, loadEmailTemplate("notify"));
	}

	public boolean isValidJWToken(String jwt) {
		String appSecretKey = CONF.appSecretKey();
		String masterSecretKey = CONF.paraSecretKey();
		return isValidJWToken(appSecretKey, jwt) || isValidJWToken(masterSecretKey, jwt);
	}

	boolean isValidJWToken(String secret, String jwt) {
		try {
			if (secret != null && jwt != null) {
				JWSVerifier verifier = new MACVerifier(secret);
				SignedJWT sjwt = SignedJWT.parse(jwt);
				if (sjwt.verify(verifier)) {
					Date referenceTime = new Date();
					JWTClaimsSet claims = sjwt.getJWTClaimsSet();

					Date expirationTime = claims.getExpirationTime();
					Date notBeforeTime = claims.getNotBeforeTime();
					String jti = claims.getJWTID();
					boolean expired = expirationTime != null && expirationTime.before(referenceTime);
					boolean notYetValid = notBeforeTime != null && notBeforeTime.after(referenceTime);
					boolean jtiRevoked = isApiKeyRevoked(jti, expired);
					return !(expired || notYetValid || jtiRevoked);
				}
			}
		} catch (JOSEException e) {
			logger.warn(null, e);
		} catch (ParseException ex) {
			logger.warn(null, ex);
		}
		return false;
	}

	public SignedJWT generateJWToken(Map<String, Object> claims) {
		return generateJWToken(claims, CONF.jwtExpiresAfterSec());
	}

	public SignedJWT generateJWToken(Map<String, Object> claims, long validitySeconds) {
		String secret = CONF.appSecretKey();
		if (!StringUtils.isBlank(secret)) {
			try {
				Date now = new Date();
				JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder();
				claimsSet.issueTime(now);
				if (validitySeconds > 0) {
					claimsSet.expirationTime(new Date(now.getTime() + (validitySeconds * 1000)));
				}
				claimsSet.notBeforeTime(now);
				claimsSet.claim(Config._APPID, CONF.paraAccessKey());
				claims.entrySet().forEach((claim) -> claimsSet.claim(claim.getKey(), claim.getValue()));
				JWSSigner signer = new MACSigner(secret);
				SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet.build());
				signedJWT.sign(signer);
				return signedJWT;
			} catch (JOSEException e) {
				logger.warn("Unable to sign JWT: {}.", e.getMessage());
			}
		}
		logger.error("Failed to generate JWT token - app_secret_key is blank.");
		return null;
	}

	public JWTClaimsSet getUnverifiedClaimsFromJWT(String jwt) {
		try {
			if (jwt != null) {
				SignedJWT sjwt = SignedJWT.parse(jwt);
				return sjwt.getJWTClaimsSet();
			}
		} catch (ParseException e) {
			logger.warn(null, e);
		}
		return null;
	}

	/**
	 * Calcuclate the TOTP code from a secret and check if it matches the one provided by the user.
	 * @param secret TOTP secret key
	 * @param code 2FA code
	 * @param variance number of 30s time frames in which 2FA codes are valid: 0 = valid once, 1 = valid for 60s, etc.
	 * @return true if codes match
	 */
	public boolean isValid2FACode(String secret, int code, int variance) {
		if (secret != null) {
			try {
				// time frame is 30 seconds
				long timeIndex = System.currentTimeMillis() / 1000 / 30;
				byte[] secretBytes = secret.replaceAll("=", "").getBytes();
				for (int i = -variance; i <= variance; i++) {
					long calculatedCode = getCode(secretBytes, timeIndex + i);
					if (calculatedCode == code) {
						return true;
					}
				}
			} catch (Exception ex) {
				logger.error(null, ex);
			}
		}
		return false;
	}

	private long getCode(byte[] secret, long timeIndex)
			throws NoSuchAlgorithmException, InvalidKeyException {
		SecretKeySpec signKey = new SecretKeySpec(secret, "HmacSHA1");
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(timeIndex);
		byte[] timeBytes = buffer.array();
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(timeBytes);
		int offset = hash[19] & 0xf;
		long truncatedHash = hash[offset] & 0x7f;
		for (int i = 1; i < 4; i++) {
			truncatedHash <<= 8;
			truncatedHash |= hash[offset + i] & 0xff;
		}
		truncatedHash = truncatedHash % 1000000;
		return truncatedHash;
	}

	public boolean isApiKeyRevoked(String jti, boolean expired) {
		if (StringUtils.isBlank(jti)) {
			return false;
		}
		loadApiKeysObject(); // prevent overwriting the API keys object
		if (API_KEYS.containsKey(jti) && expired) {
			revokeApiKey(jti);
		}
		return !API_KEYS.containsKey(jti);
	}

	public void registerApiKey(String jti, String jwt) {
		if (StringUtils.isBlank(jti) || StringUtils.isBlank(jwt)) {
			return;
		}
		loadApiKeysObject(); // prevent overwriting the API keys object
		API_KEYS.put(jti, jwt);
		saveApiKeysObject();
	}

	public void revokeApiKey(String jti) {
		loadApiKeysObject(); // prevent overwriting the API keys object
		API_KEYS.remove(jti);
		saveApiKeysObject();
	}

	public Map<String, Object> getApiKeys() {
		return loadApiKeysObject();
	}

	public Map<String, Long> getApiKeysExpirations() {
		return API_KEYS.keySet().stream().collect(Collectors.toMap(k -> k, k -> {
			String jwt = (String) API_KEYS.get(k);
			try {
				if (!StringUtils.isBlank(jwt)) {
					Date exp = SignedJWT.parse(jwt).getJWTClaimsSet().getExpirationTime();
					if (exp != null) {
						return exp.getTime();
					}
				}
			} catch (Exception ex) {
				logger.error("Failed to parse API key " + k + " - key doesn't seem to be in JWT format. {}", ex.getMessage());
			}
			return 0L;
		}));
	}

	private void saveApiKeysObject() {
		Sysprop s = new Sysprop("api_keys");
		s.setProperties(API_KEYS);
		pc.create(s);
	}

	private Map<String, Object> loadApiKeysObject() {
		if (API_KEYS.isEmpty()) {
			Sysprop s = pc.read("api_keys");
			if (s != null) {
				API_KEYS.putAll(s.getProperties());
			}
		}
		return API_KEYS;
	}

	public Profile getSystemUser() {
		return API_USER;
	}

	public void triggerHookEvent(String eventName, Object payload) {
		if (isWebhooksEnabled() && HOOK_EVENTS.contains(eventName)) {
			Para.asyncExecute(() -> {
				Webhook trigger = new Webhook();
				trigger.setTriggeredEvent(eventName);
				trigger.setCustomPayload(payload);
				pc.create(trigger);
			});
		}
	}

	public void setSecurityHeaders(String nonce, HttpServletRequest request, HttpServletResponse response) {
		// CSP Header
		if (CONF.cspHeaderEnabled()) {
			response.setHeader("Content-Security-Policy",
					(request.isSecure() ? "upgrade-insecure-requests; " : "") + CONF.cspHeader(nonce));
		}
		// HSTS Header
		if (CONF.hstsHeaderEnabled()) {
			response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		}
		// Frame Options Header
		if (CONF.framingHeaderEnabled()) {
			response.setHeader("X-Frame-Options", "SAMEORIGIN");
		}
		// XSS Header
		if (CONF.xssHeaderEnabled()) {
			response.setHeader("X-XSS-Protection", "1; mode=block");
		}
		// Content Type Header
		if (CONF.contentTypeHeaderEnabled()) {
			response.setHeader("X-Content-Type-Options", "nosniff");
		}
		// Referrer Header
		if (CONF.referrerHeaderEnabled()) {
			response.setHeader("Referrer-Policy", "strict-origin");
		}
		// Permissions Policy Header
		if (CONF.permissionsHeaderEnabled()) {
			response.setHeader("Permissions-Policy", "geolocation=()");
		}
	}

	public boolean cookieConsentGiven(HttpServletRequest request) {
		return !CONF.cookieConsentRequired() || "allow".equals(HttpUtils.getCookieValue(request, "cookieconsent_status"));
	}

	public String base64DecodeScript(String encodedScript) {
		if (StringUtils.isBlank(encodedScript)) {
			return "";
		}
		try {
			String decodedScript = Base64.isBase64(encodedScript) ? Utils.base64dec(encodedScript) : "";
			return StringUtils.isBlank(decodedScript) ? encodedScript : decodedScript;
		} catch (Exception e) {
			return encodedScript;
		}
	}

	public Map<String, Object> getExternalScripts() {
		return CONF.externalScripts();
	}

	public List<String> getExternalStyles() {
		String extStyles = CONF.externalStyles();
		if (!StringUtils.isBlank(extStyles)) {
			String[] styles = extStyles.split("\\s*,\\s*");
			if (!StringUtils.isBlank(extStyles) && styles != null && styles.length > 0) {
				ArrayList<String> list = new ArrayList<String>();
				for (String style : styles) {
					if (!StringUtils.isBlank(style)) {
						list.add(style);
					}
				}
				return list;
			}
		}
		return Collections.emptyList();
	}

	public String getInlineCSS() {
		try {
			Sysprop custom = getCustomTheme();
			String themeName = custom.getName();
			String inline = CONF.inlineCSS();
			String loadedTheme;
			if ("default".equalsIgnoreCase(themeName) || StringUtils.isBlank(themeName)) {
				return inline;
			} else if ("custom".equalsIgnoreCase(themeName)) {
				loadedTheme = (String) custom.getProperty("theme");
			} else {
				loadedTheme = loadResource(getThemeKey(themeName));
				if (StringUtils.isBlank(loadedTheme)) {
					FILE_CACHE.put("theme", "default");
					custom.setName("default");
					customTheme = pc.update(custom);
					return inline;
				} else {
					FILE_CACHE.put("theme", themeName);
				}
			}
			loadedTheme = StringUtils.replaceEachRepeatedly(loadedTheme,
					new String[] {"<", "</", "<script", "<SCRIPT"}, new String[] {"", "", "", ""});
			return loadedTheme + "\n/*** END OF THEME CSS ***/\n" + inline;
		} catch (Exception e) {
			logger.debug("Failed to load inline CSS.");
		}
		return "";
	}

	public void setCustomTheme(String themeName, String themeCSS) {
		String id = "theme" + Para.getConfig().separator() + "custom";
		boolean isCustom = "custom".equalsIgnoreCase(themeName);
		String css = isCustom ? themeCSS : "";
		Sysprop custom = new Sysprop(id);
		custom.setName(StringUtils.isBlank(css) && isCustom ? "default" : themeName);
		custom.addProperty("theme", css);
		customTheme = pc.create(custom);
		FILE_CACHE.put("theme", themeName);
		FILE_CACHE.put(getThemeKey(themeName), isCustom ? css : loadResource(getThemeKey(themeName)));
	}

	public Sysprop getCustomTheme() {
		String id = "theme" + Para.getConfig().separator() + "custom";
		if (customTheme == null) {
			customTheme = (Sysprop) Optional.ofNullable(pc.read(id)).orElseGet(this::getDefaultThemeObject);
		}
		return customTheme;
	}

	private Sysprop getDefaultThemeObject() {
		String themeName = "default";
		Sysprop s = new Sysprop("theme" + Para.getConfig().separator() + "custom");
		s.setName(themeName);
		s.addProperty("theme", "");
		FILE_CACHE.put("theme", themeName);
		FILE_CACHE.put(getThemeKey(themeName), loadResource(getThemeKey(themeName)));
		return s;
	}

	private String getThemeKey(String themeName) {
		return "themes/" + themeName + ".css";
	}

	public String getDefaultTheme() {
		return loadResource("themes/default.css");
	}

	public String getLogoUrl(Profile authUser, HttpServletRequest req) {
		return isDarkModeEnabled(authUser, req) ? CONF.logoDarkUrl() : CONF.logoUrl();
	}

	public String getSmallLogoUrl() {
		String defaultLogo = CONF.serverUrl() + CONF.imagesLink() + "/logowhite.png";
		String logoUrl = CONF.logoSmallUrl();
		String defaultMainLogoUrl = CONF.imagesLink() + "/logo.svg";
		String mainLogoUrl = CONF.logoUrl();
		if (!defaultLogo.equals(logoUrl)) {
			return logoUrl;
		} else if (!mainLogoUrl.equals(defaultMainLogoUrl)) {
			return mainLogoUrl;
		}
		return logoUrl;
	}

	public boolean isPasswordStrongEnough(String password) {
		if (StringUtils.length(password) >= CONF.minPasswordLength()) {
			int score = 0;
			if (password.matches(".*[a-z].*")) {
				score++;
			}
			if (password.matches(".*[A-Z].*")) {
				score++;
			}
			if (password.matches(".*[0-9].*")) {
				score++;
			}
			if (password.matches(".*[^\\w\\s\\n\\t].*")) {
				score++;
			}
			// 1 = good strength, 2 = medium strength, 3 = high strength
			if (CONF.minPasswordStrength() <= 1) {
				return score >= 2;
			} else if (CONF.minPasswordStrength() == 2) {
				return score >= 3;
			} else {
				return score >= 4;
			}
		}
		return false;
	}

	public AkismetComment buildAkismetComment(ParaObject pobj, Profile authUser, HttpServletRequest req) {
		if (pobj == null) {
			return null;
		}
		final AkismetComment comment = new AkismetComment(req);
		if (pobj instanceof Comment) {
			comment.setContent(((Comment) pobj).getComment());
			comment.setPermalink(CONF.serverUrl() + "/comment/" + ((Comment) pobj).getId());
			comment.setType("comment");
		} else if (pobj instanceof Post) {
			comment.setContent(((Post) pobj).getTitle() + " \n " + ((Post) pobj).getBody());
			comment.setPermalink(CONF.serverUrl() + ((Post) pobj).getPostLinkForRedirect());
			comment.setType(((Post) pobj).isReply() ? "reply" : "forum‑post");
		}
		if (authUser != null) {
			comment.setAuthor(authUser.getName());
			User u = authUser.getUser();
			if (u != null) {
				comment.setAuthorEmail(u.getEmail());
			}
		}
		comment.setUserRole(isMod(authUser) ? "administrator" : null);
		return comment;
	}

	public AkismetComment buildAkismetCommentFromReport(Report rep, HttpServletRequest req) {
		if (rep == null) {
			return null;
		}
		final AkismetComment comment = new AkismetComment(req);
		comment.setContent(rep.getContent());
		comment.setPermalink(rep.getLink());
		comment.setType(rep.getLink().contains("/comment/") ? "comment" : "forum-post");

		Profile authUser = pc.read(rep.getCreatorid());
		if (authUser != null) {
			comment.setAuthor(authUser.getName());
			User u = authUser.getUser();
			if (u != null) {
				comment.setAuthorEmail(u.getEmail());
			}
		}
		comment.setUserRole(isMod(authUser) ? "administrator" : null);
		return comment;
	}

	public boolean isSpam(ParaObject pobj, Profile authUser, HttpServletRequest req) {
		if (pobj == null || StringUtils.isBlank(CONF.akismetApiKey())) {
			return false;
		}
		final AkismetComment comment = buildAkismetComment(pobj, authUser, req);
		Akismet akismet = new Akismet(CONF.akismetApiKey(), CONF.serverUrl());
		final boolean isSpam = akismet.checkComment(comment);
		confirmSpam(comment, isSpam, CONF.automaticSpamProtectionEnabled(), req);
		return isSpam;
	}

	public void confirmSpam(AkismetComment comment, boolean isSpam, boolean submit, HttpServletRequest req) {
		Akismet akismet = new Akismet(CONF.akismetApiKey(), CONF.serverUrl());
		if (isSpam) {
			if (submit) {
				boolean err = akismet.submitSpam(comment);
				if (err) {
					logger.error("Failed to confirm spam to Akismet: " +
							akismet.getResponse() + " " + akismet.getErrorMessage());
				} else {
					logger.info("Detected spam post by user {} which was blocked, URL {}.",
							comment.getAuthor(), comment.getPermalink());
				}
			}
		} else {
			if (submit) {
				boolean err = akismet.submitHam(comment);
				if (err) {
					logger.error(akismet.getErrorMessage());
				}
			}
		}
	}

	public String getCSPNonce() {
		return Utils.generateSecurityToken(16);
	}

	public String getFacebookLoginURL() {
		return "https://www.facebook.com/dialog/oauth?client_id=" + CONF.facebookAppId() +
				"&response_type=code&scope=email&state=" + getStateParam() +
				"&redirect_uri=" + getParaEndpoint() + "/facebook_auth";
	}

	public String getGoogleLoginURL() {
		return "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + CONF.googleAppId() +
				"&response_type=code&scope=openid%20profile%20email&state=" + getStateParam() +
				"&redirect_uri=" + getParaEndpoint() + "/google_auth";
	}

	public String getGitHubLoginURL() {
		return "https://github.com/login/oauth/authorize?response_type=code&client_id=" + CONF.githubAppId() +
				"&scope=user%3Aemail&state=" + getStateParam() +
				"&redirect_uri=" + getParaEndpoint() + "/github_auth";
	}

	public String getLinkedInLoginURL() {
		return "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=" + CONF.linkedinAppId() +
				"&scope=r_liteprofile%20r_emailaddress&state=" + getStateParam() +
				"&redirect_uri=" + getParaEndpoint() + "/linkedin_auth";
	}

	public String getTwitterLoginURL() {
		return getParaEndpoint() + "/twitter_auth?state=" + getStateParam();
	}

	public String getMicrosoftLoginURL() {
		return "https://login.microsoftonline.com/" + CONF.microsoftTenantId() +
				"/oauth2/v2.0/authorize?response_type=code&client_id=" + CONF.microsoftAppId() +
				"&scope=https%3A%2F%2Fgraph.microsoft.com%2Fuser.read&state=" + getStateParam() +
				"&redirect_uri=" + getParaEndpoint() + "/microsoft_auth";
	}

	public String getSlackLoginURL() {
		return "https://slack.com/oauth/v2/authorize?response_type=code&client_id=" + CONF.slackAppId() +
				"&user_scope=identity.basic%20identity.email%20identity.team%20identity.avatar&state=" + getStateParam() +
				"&redirect_uri=" + getParaEndpoint() + "/slack_auth";
	}

	public String getAmazonLoginURL() {
		return "https://www.amazon.com/ap/oa?response_type=code&client_id=" + CONF.amazonAppId() +
				"&scope=profile&state=" + getStateParam() +
				"&redirect_uri=" + getParaEndpoint() + "/amazon_auth";
	}

	public String getOAuth2LoginURL() {
		return CONF.oauthAuthorizationUrl("") + "?" +
				"response_type=code&client_id=" + CONF.oauthAppId("") +
				"&scope=" + CONF.oauthScope("") + getOauth2StateParam("") +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth" + getOauth2AppidParam("");
	}

	public String getOAuth2SecondLoginURL() {
		return CONF.oauthAuthorizationUrl("second") + "?" +
				"response_type=code&client_id=" + CONF.oauthAppId("second") +
				"&scope=" +  CONF.oauthScope("second") + getOauth2StateParam("second") +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth" + getOauth2AppidParam("second");
	}

	public String getOAuth2ThirdLoginURL() {
		return CONF.oauthAuthorizationUrl("third") + "?" +
				"response_type=code&client_id=" + CONF.oauthAppId("third") +
				"&scope=" +  CONF.oauthScope("third") + getOauth2StateParam("third") +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth" + getOauth2AppidParam("third");
	}

	public String getParaEndpoint() {
		return CONF.redirectUri();
	}

	public String getParaAppId() {
		return StringUtils.removeStart(CONF.paraAccessKey(), "app:");
	}

	private String getOauth2StateParam(String a) {
		return "&state=" + (CONF.oauthAppidInStateParamEnabled(a) ? getStateParam() : UUID.randomUUID().toString());
	}

	private String getOauth2AppidParam(String a) {
		return CONF.oauthAppidInStateParamEnabled(a) ? "" : "?appid=" + getParaAppId();
	}

	private String getStateParam() {
		if (StringUtils.isBlank(CONF.hostUrlAliases())) {
			return getParaAppId();
		} else {
			int index = Arrays.asList(CONF.hostUrlAliases().split("\\s*,\\s*")).indexOf(CONF.serverUrl());
			return getParaAppId() + (index >= 0 ? "|" + index : "");
		}
	}

	public String getFirstConfiguredLoginURL() {
		if (!CONF.facebookAppId().isEmpty()) {
			return getFacebookLoginURL();
		}
		if (!CONF.googleAppId().isEmpty()) {
			return getGoogleLoginURL();
		}
		if (!CONF.githubAppId().isEmpty()) {
			return getGitHubLoginURL();
		}
		if (!CONF.linkedinAppId().isEmpty()) {
			return getLinkedInLoginURL();
		}
		if (!CONF.twitterAppId().isEmpty()) {
			return getTwitterLoginURL();
		}
		if (isMicrosoftAuthEnabled()) {
			return getMicrosoftLoginURL();
		}
		if (isSlackAuthEnabled()) {
			return getSlackLoginURL();
		}
		if (!CONF.amazonAppId().isEmpty()) {
			return getAmazonLoginURL();
		}
		if (!CONF.oauthAppId("").isEmpty()) {
			return getOAuth2LoginURL();
		}
		if (!CONF.oauthAppId("second").isEmpty()) {
			return getOAuth2SecondLoginURL();
		}
		if (!CONF.oauthAppId("third").isEmpty()) {
			return getOAuth2ThirdLoginURL();
		}
		return SIGNINLINK + "?code=3&error=true";
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.email.Emailer;
import com.erudika.para.core.utils.Para;
import com.erudika.scoold.ScooldConfig;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * A simple JavaMail implementation of {@link Emailer}.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class ScooldEmailer implements Emailer {

	private static final Logger logger = LoggerFactory.getLogger(ScooldEmailer.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private JavaMailSender mailSender;

	public ScooldEmailer(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public boolean sendEmail(final List<String> emails, final String subject, final String body) {
		if (emails == null || emails.isEmpty()) {
			return false;
		}
		Para.asyncExecute(() -> {
			emails.forEach(email -> {
				try {
					mailSender.send((MimeMessage mimeMessage) -> {
						MimeMessageHelper msg = new MimeMessageHelper(mimeMessage);
						msg.setTo(email);
						msg.setSubject(subject);
						msg.setFrom(CONF.supportEmail(), CONF.appName());
						msg.setText(body, true); // body is assumed to be HTML
					});
					logger.debug("Email sent to {}, {}", email, subject);
				} catch (MailException ex) {
					logger.error("Failed to send email to {} with body [{}]. {}", email, body, ex.getMessage());
				}
			});
		});
		return true;
	}


	@Override
	public boolean sendEmail(List<String> emails, String subject, String body, InputStream attachment, String mimeType, String fileName) {
		if (emails == null || emails.isEmpty()) {
			return false;
		}
		Para.asyncExecute(() -> {
			MimeMessagePreparator preparator = (MimeMessage mimeMessage) -> {
				MimeMessageHelper msg = new MimeMessageHelper(mimeMessage);
				Iterator<String> emailz = emails.iterator();
				msg.setTo(emailz.next());
				while (emailz.hasNext()) {
					msg.addBcc(emailz.next());
				}
				msg.setSubject(subject);
				msg.setFrom(Para.getConfig().supportEmail());
				msg.setText(body, true); // body is assumed to be HTML
				if (attachment != null) {
					msg.addAttachment(fileName, new ByteArrayDataSource(attachment, mimeType));
				}
			};
			try {
				mailSender.send(preparator);
				logger.debug("Email sent to {}, {}", emails, subject);
			} catch (MailException ex) {
				logger.error("Failed to send email. {}", ex.getMessage());
			}
		});
		return true;
	}
}

/*
 * Copyright 2013-2024 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.Address;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Vote;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.core.Badge;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.Sticky;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Core utils.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@SuppressWarnings("unchecked")
public final class CoreUtils {

	private static final Set<String> CORE_TYPES;

	private CoreUtils() { }

	static {
		CORE_TYPES = new HashSet<>(Arrays.asList(
				Utils.type(Badge.class),
				Utils.type(Comment.class),
				Utils.type(Feedback.class),
				Utils.type(Profile.class),
				Utils.type(Question.class),
				Utils.type(Reply.class),
				Utils.type(Report.class),
				Utils.type(Revision.class),
				//Utils.type(Sticky.class),
				Utils.type(UnapprovedQuestion.class),
				Utils.type(UnapprovedReply.class),
				// Para core types
				Utils.type(Address.class),
				Utils.type(Sysprop.class),
				Utils.type(Tag.class),
				Utils.type(User.class),
				Utils.type(Vote.class)
		));
	}

	public static void registerCoreClasses() {
		Para.registerCoreClasses(
				Badge.class,
				Comment.class,
				Feedback.class,
				Profile.class,
				Question.class,
				Reply.class,
				Report.class,
				Revision.class,
				Sticky.class,
				UnapprovedQuestion.class,
				UnapprovedReply.class);
	}

	public static Set<String> getCoreTypes() {
		return Set.copyOf(CORE_TYPES);
	}

}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class UnauthorizedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnauthorizedException() {
		super("401 Unauthorized");
	}

}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Translation;
import com.erudika.para.core.utils.Para;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility class for language operations.
 * These can be used to build a crowdsourced translation system.
 * @author Alex Bogdanovski [alex@erudika.com]
 * @see Translation
 */
@Component
@Singleton
public class LanguageUtils {

	private static final Logger logger = LoggerFactory.getLogger(LanguageUtils.class);

	private static final Map<String, Locale> ALL_LOCALES = new HashMap<String, Locale>();
	static {
		for (Locale loc : LocaleUtils.availableLocaleList()) {
			String locstr = loc.getLanguage();
			if (!StringUtils.isBlank(locstr)) {
				ALL_LOCALES.putIfAbsent(locstr, Locale.forLanguageTag(locstr));
			}
		}
		ALL_LOCALES.remove("zh");
		ALL_LOCALES.putIfAbsent(Locale.SIMPLIFIED_CHINESE.toString(), Locale.SIMPLIFIED_CHINESE);
		ALL_LOCALES.putIfAbsent(Locale.TRADITIONAL_CHINESE.toString(), Locale.TRADITIONAL_CHINESE);
	}

	private static final Map<String, Map<String, String>> LANG_CACHE =
			new ConcurrentHashMap<String, Map<String, String>>(ALL_LOCALES.size());

	private static final Map<String, Integer> LANG_PROGRESS_CACHE = new HashMap<String, Integer>(ALL_LOCALES.size());

	private final String keyPrefix = "language".concat(Para.getConfig().separator());
	private final ParaClient pc;

	/**
	 * Default constructor.
	 * @param pc ParaClient
	 */
	@Inject
	public LanguageUtils(ParaClient pc) {
		this.pc = pc;
	}

	/**
	 * Reads localized strings from a file first, then the DB if a file is not found.
	 * Returns a map of all translations for a given language.
	 * Defaults to the default language which must be set.
	 * @param langCode the 2-letter language code
	 * @return the language map
	 */
	public Map<String, String> readLanguage(String langCode) {
		if (StringUtils.isBlank(langCode) || langCode.equals(getDefaultLanguageCode())) {
			return getDefaultLanguage();
		} else if (langCode.length() > 2 && !ALL_LOCALES.containsKey(langCode)) {
			return readLanguage(langCode.substring(0, 2));
		} else if (LANG_CACHE.containsKey(langCode)) {
			return LANG_CACHE.get(langCode);
		}

		// load language map from file
		Map<String, String> lang = readLanguageFromFileAndUpdateProgress(langCode);
		if (lang == null || lang.isEmpty()) {
			// or try to load from DB
			lang = new TreeMap<String, String>(getDefaultLanguage());
			Sysprop s = pc.read(keyPrefix.concat(langCode));
			if (s != null && !s.getProperties().isEmpty()) {
				Map<String, Object> loaded = s.getProperties();
				for (Map.Entry<String, String> entry : lang.entrySet()) {
					if (loaded.containsKey(entry.getKey())) {
						lang.put(entry.getKey(), String.valueOf(loaded.get(entry.getKey())));
					} else {
						lang.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		LANG_CACHE.put(langCode, lang);
		return Map.copyOf(lang);
	}

	/**
	 * Returns a non-null locale for a given language code.
	 * @param langCode the 2-letter language code
	 * @return a locale. default is English
	 */
	public Locale getProperLocale(String langCode) {
		if (StringUtils.startsWith(langCode, "zh")) {
			if ("zh_tw".equalsIgnoreCase(langCode)) {
				return Locale.TRADITIONAL_CHINESE;
			} else {
				return Locale.SIMPLIFIED_CHINESE;
			}
		}
		String lang = StringUtils.substring(langCode, 0, 2);
		lang = (StringUtils.isBlank(lang) || !ALL_LOCALES.containsKey(lang)) ? "en" : lang.trim().toLowerCase();
		return ALL_LOCALES.get(lang);
	}

	/**
	 * Returns the default language map.
	 * @return the default language map or an empty map if the default isn't set.
	 */
	public Map<String, String> getDefaultLanguage() {
		if (!LANG_CACHE.containsKey(getDefaultLanguageCode())) {
			// initialize the language cache maps
			LANG_CACHE.put(getDefaultLanguageCode(), readLanguageFromFileAndUpdateProgress(getDefaultLanguageCode()));
		}
		return LANG_CACHE.get(getDefaultLanguageCode());
	}

	/**
	 * Returns the default language code.
	 * @return the 2-letter language code
	 */
	public String getDefaultLanguageCode() {
		return "en";
	}

	/**
	 * Returns a map of language codes and the percentage of translated string for that language.
	 * @return a map indicating translation progress
	 */
	public Map<String, Integer> getTranslationProgressMap() {
		if (!LANG_PROGRESS_CACHE.isEmpty() && LANG_PROGRESS_CACHE.size() > 2) { // en + default user lang
			return LANG_PROGRESS_CACHE;
		}
		for (String langCode : ALL_LOCALES.keySet()) {
			if (!langCode.equals(getDefaultLanguageCode())) {
				LANG_CACHE.put(langCode, readLanguageFromFileAndUpdateProgress(langCode));
			}
		}
		return LANG_PROGRESS_CACHE;
	}

	/**
	 * Returns a map of all language codes and their locales.
	 * @return a map of language codes to locales
	 */
	public Map<String, Locale> getAllLocales() {
		return ALL_LOCALES;
	}

	private int calculateProgressPercent(double approved, double defsize) {
		// allow 5 identical words per language (i.e. Email, etc)
		if (approved >= defsize - 10) {
			approved = defsize;
		}
		if (defsize == 0) {
			return 0;
		} else {
			return (int) ((approved / defsize) * 100.0);
		}
	}

	private Map<String, String> readLanguageFromFileAndUpdateProgress(String langCode) {
		if (langCode != null) {
			Properties lang = new Properties();
			String file = "lang_" + langCode.toLowerCase() + ".properties";
			try (InputStream ins = LanguageUtils.class.getClassLoader().getResourceAsStream(file)) {
				if (ins != null) {
					lang.load(ins);
					int progress = 0;
					Map<String, String> langmap = new TreeMap<String, String>();
					Set<String> keySet = langCode.equalsIgnoreCase(getDefaultLanguageCode()) ?
							lang.stringPropertyNames() : getDefaultLanguage().keySet();
					for (String propKey : keySet) {
						String propVal = lang.getProperty(propKey);
						if (!langCode.equalsIgnoreCase(getDefaultLanguageCode())) {
							String defaultVal = getDefaultLanguage().get(propKey);
							if (!StringUtils.isBlank(propVal) && !StringUtils.equalsIgnoreCase(propVal, defaultVal)) {
								progress++;
							} else if (StringUtils.isBlank(propVal)) {
								propVal = defaultVal;
							}
						}
						langmap.put(propKey, propVal);
					}
					if (langCode.equalsIgnoreCase(getDefaultLanguageCode())) {
						progress = langmap.size(); // 100%
					}
					LANG_PROGRESS_CACHE.put(langCode, calculateProgressPercent(progress, langmap.size()));
					return langmap;
				}
			} catch (Exception e) {
				logger.info("Could not read language file " + file + ": ", e);
			}
		}
		return Collections.emptyMap();
	}
}

/*
 * Copyright 2013-2022 Erudika. http://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.User;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import com.erudika.scoold.core.Profile;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various utilities for HTTP stuff - cookies, AJAX, etc.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class HttpUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	public static final String TWO_FA_COOKIE = CONF.authCookie() + "-2fa";
	private static CloseableHttpClient httpclient;
	private static final String DEFAULT_AVATAR = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
			+ "<svg xmlns=\"http://www.w3.org/2000/svg\" id=\"svg8\" width=\"756\" height=\"756\" "
			+ "version=\"1\" viewBox=\"0 0 200 200\">\n"
			+ "  <g id=\"layer1\" transform=\"translate(0 -97)\">\n"
			+ "    <rect id=\"rect1433\" width=\"282\" height=\"245\" x=\"-34\" y=\"79\" fill=\"#ececec\" rx=\"2\"/>\n"
			+ "  </g>\n"
			+ "  <g id=\"layer2\" fill=\"gray\">\n"
			+ "    <circle id=\"path1421\" cx=\"102\" cy=\"-70\" r=\"42\" transform=\"scale(1 -1)\"/>\n"
			+ "    <ellipse id=\"path1423\" cx=\"101\" cy=\"201\" rx=\"71\" ry=\"95\"/>\n"
			+ "  </g>\n"
			+ "</svg>";

	/**
	 * Default private constructor.
	 */
	private HttpUtils() { }

	static CloseableHttpClient getHttpClient() {
		if (httpclient == null) {
			int timeout = 5;
			httpclient = HttpClientBuilder.create().
//					setConnectionReuseStrategy(new NoConnectionReuseStrategy()).
//					setRedirectStrategy(new LaxRedirectStrategy()).
					setDefaultRequestConfig(RequestConfig.custom().
//							setConnectTimeout(timeout, TimeUnit.SECONDS).
							setConnectionRequestTimeout(timeout, TimeUnit.SECONDS).
							build()).
					build();
		}
		return httpclient;
	}

	/**
	 * Checks if a request comes from JavaScript.
	 * @param request HTTP request
	 * @return true if AJAX
	 */
	public static boolean isAjaxRequest(HttpServletRequest request) {
		return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With")) ||
				"XMLHttpRequest".equalsIgnoreCase(request.getParameter("X-Requested-With"));
	}

	/////////////////////////////////////////////
	//    	   COOKIE & STATE UTILS
	/////////////////////////////////////////////

	/**
	 * Sets a cookie.
	 * @param name the name
	 * @param value the value
	 * @param req HTTP request
	 * @param res HTTP response
	 */
	public static void setStateParam(String name, String value, HttpServletRequest req, HttpServletResponse res) {
		setStateParam(name, value, req, res, false);
	}

	/**
	 * Sets a cookie.
	 * @param name the name
	 * @param value the value
	 * @param req HTTP request
	 * @param res HTTP response
	 * @param httpOnly HTTP only flag
	 */
	public static void setStateParam(String name, String value, HttpServletRequest req,
			HttpServletResponse res, boolean httpOnly) {
		setRawCookie(name, value, req, res, null, -1);
	}

	/**
	 * Reads a cookie.
	 * @param name the name
	 * @param req HTTP request
	 * @return the cookie value
	 */
	public static String getStateParam(String name, HttpServletRequest req) {
		return getCookieValue(req, name);
	}

	/**
	 * Deletes a cookie.
	 * @param name the name
	 * @param req HTTP request
	 * @param res HTTP response
	 */
	public static void removeStateParam(String name, HttpServletRequest req,
			HttpServletResponse res) {
		setRawCookie(name, "", req, res, null, 0);
	}

	/**
	 * Sets a cookie.
	 * @param name the name
	 * @param value the value
	 * @param req HTTP request
	 * @param res HTTP response
	 * @param sameSite SameSite flag
	 * @param maxAge max age
	 */
	public static void setRawCookie(String name, String value, HttpServletRequest req,
			HttpServletResponse res, String sameSite, int maxAge) {
		if (StringUtils.isBlank(name) || value == null || req == null || res == null) {
			return;
		}
		String expires = DateFormatUtils.format(System.currentTimeMillis() + (maxAge * 1000),
				"EEE, dd-MMM-yyyy HH:mm:ss z", TimeZone.getTimeZone("GMT"));
		String path = CONF.serverContextPath().isEmpty() ? "/" : CONF.serverContextPath();
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("=").append(value).append(";");
		sb.append("Path=").append(path).append(";");
		sb.append("Expires=").append(expires).append(";");
		sb.append("Max-Age=").append(maxAge < 0 ? CONF.sessionTimeoutSec() : maxAge).append(";");
		sb.append("HttpOnly;"); // all cookies should be HttpOnly, JS does not need to read cookie values
		if (StringUtils.startsWithIgnoreCase(CONF.serverUrl(), "https://") || req.isSecure()) {
			sb.append("Secure;");
		}
		if (!StringUtils.isBlank(sameSite)) {
			sb.append("SameSite=").append(sameSite);
		}
		res.addHeader(jakarta.ws.rs.core.HttpHeaders.SET_COOKIE, sb.toString());
	}

	/**
	 * Reads a cookie.
	 * @param name the name
	 * @param req HTTP request
	 * @return the cookie value
	 */
	public static String getCookieValue(HttpServletRequest req, String name) {
		if (StringUtils.isBlank(name) || req == null) {
			return null;
		}
		Cookie[] cookies = req.getCookies();
		if (cookies == null) {
			return null;
		}
		//Otherwise, we have to do a linear scan for the cookie.
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static String getFullUrl(HttpServletRequest req) {
		return getFullUrl(req, false);
	}

	public static String getFullUrl(HttpServletRequest req, boolean relative) {
		String queryString = req.getQueryString();
		String url = req.getRequestURL().toString();
		if (queryString != null) {
			url = req.getRequestURL().append('?').append(queryString).toString();
		}
		if (relative) {
			url = "/" + URI.create(CONF.serverUrl()).relativize(URI.create(url)).toString();
		}
		return url;
	}

	/**
	 * @param token CAPTCHA
	 * @return boolean
	 */
	public static boolean isValidCaptcha(String token) {
		if (StringUtils.isBlank(CONF.captchaSecretKey())) {
			return true;
		}
		if (StringUtils.isBlank(token)) {
			return false;
		}
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("secret", CONF.captchaSecretKey()));
		params.add(new BasicNameValuePair("response", token));
		HttpPost post = new HttpPost("https://www.google.com/recaptcha/api/siteverify");
		post.setEntity(new UrlEncodedFormEntity(params));
		try {
			return HttpUtils.getHttpClient().execute(post, (resp) -> {
				if (resp.getCode() == HttpStatus.SC_OK && resp.getEntity() != null) {
					Map<String, Object> data = ParaObjectUtils.getJsonReader(Map.class).readValue(resp.getEntity().getContent());
					if (data != null && data.containsKey("success")) {
						return (boolean) data.getOrDefault("success", false);
					}
				}
				return false;
			});
		} catch (Exception ex) {
			LoggerFactory.getLogger(HttpUtils.class).debug("Failed to verify CAPTCHA: {}", ex.getMessage());
		}
		return false;
	}

	public static void getDefaultAvatarImage(HttpServletResponse res) {
		try {
			res.setContentType("image/svg+xml");
			res.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + TimeUnit.HOURS.toSeconds(24));
			res.setHeader(HttpHeaders.ETAG, Utils.md5(DEFAULT_AVATAR));
			IOUtils.copy(new ByteArrayInputStream(DEFAULT_AVATAR.getBytes()), res.getOutputStream());
		} catch (IOException e) {
			LoggerFactory.getLogger(HttpUtils.class).
					debug("Failed to set default user avatar. {}", e.getMessage());
		}
	}

	/**
	 * Sets the session cookie.
	 * @param authUser auth user
	 * @param req req
	 * @param res res
	 */
	public static void setAuthCookie(User authUser, HttpServletRequest req, HttpServletResponse res) {
		if (!StringUtils.isBlank(authUser.getPassword())) {
			setRawCookie(CONF.authCookie(), authUser.getPassword(), req, res, "Lax", CONF.sessionTimeoutSec());
		}
		try {
			Profile authu = ScooldUtils.getInstance().getParaClient().read(Profile.id(authUser.getId()));
			if (authu != null && !StringUtils.isBlank(authu.getPreferredSpace())) {
				ScooldUtils.getInstance().storeSpaceIdInCookie(authu.getPreferredSpace(), req, res);
			}
		} catch (Exception ex) {
			logger.error(null, ex);
		}
	}

	/**
	 * Sets the 2FA cookie.
	 * @param authUser auth user
	 * @param loginTime login time
	 * @param req req
	 * @param res res
	 */
	public static void set2FACookie(User authUser, Date loginTime, HttpServletRequest req, HttpServletResponse res) {
		if (authUser != null && !StringUtils.isBlank(authUser.getTwoFAkey())) {
			String cookieValue = Utils.hmacSHA256(authUser.getId(),
					loginTime.getTime() + Para.getConfig().separator() + authUser.getTwoFAkey());
			setRawCookie(TWO_FA_COOKIE, cookieValue, req, res, "Lax", CONF.sessionTimeoutSec());
		} else {
			removeStateParam(TWO_FA_COOKIE, req, res);
		}
	}

	/**
	 * Checks the validity of the 2FA cookie.
	 * @param authUser auth user
	 * @param loginTime login time
	 * @param req req
	 * @param res res
	 * @return true if valid
	 */
	public static boolean isValid2FACookie(User authUser, Date loginTime, HttpServletRequest req, HttpServletResponse res) {
		String twoFACookie = getStateParam(TWO_FA_COOKIE, req);
		if (!StringUtils.isBlank(twoFACookie) && authUser != null) {
			String computed = Utils.hmacSHA256(authUser.getId(),
					loginTime.getTime() + Para.getConfig().separator() + authUser.getTwoFAkey());
			return StringUtils.equals(computed, twoFACookie);
		}
		return false;
	}

	/**
	 * @param req req
	 * @return the original protected URL visited before authentication
	 */
	public static String getBackToUrl(HttpServletRequest req) {
		return getBackToUrl(req, false);
	}

	/**
	 * @param req req
	 * @param relative relative
	 * @return the original protected URL visited before authentication
	 */
	public static String getBackToUrl(HttpServletRequest req, boolean relative) {
		String backto = Optional.ofNullable(StringUtils.stripToNull(req.getParameter("returnto"))).
				orElse(Utils.urlDecode(HttpUtils.getStateParam("returnto", req)));
		String serverUrl = CONF.serverUrl() + CONF.serverContextPath();
		String resolved = "";
		try {
			resolved = URI.create(serverUrl).resolve(Optional.ofNullable(backto).orElse("")).toString();
		} catch (Exception e) {
			logger.warn("Invalid return-to URI: {}", e.getMessage());
		}
		if (!StringUtils.startsWithIgnoreCase(resolved, serverUrl)) {
			backto = "";
		} else {
			backto = resolved;
		}
		if (relative) {
			backto = "/" + URI.create(CONF.serverUrl()).relativize(URI.create(backto)).toString();
		}
		return (StringUtils.isBlank(backto) ? HOMEPAGE : backto);
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.scoold.core.Profile;

public interface AvatarRepository {
	String getLink(Profile profile, AvatarFormat format);
	String getAnonymizedLink(String data);

	boolean store(Profile profile, String url);
}


/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import org.apache.commons.lang3.StringUtils;


public class ImgurAvatarRepository implements AvatarRepository {

	private final AvatarRepository nextRepository;

	public ImgurAvatarRepository(AvatarRepository nextRepository) {
		this.nextRepository = nextRepository;
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		if (profile == null || StringUtils.isBlank(profile.getPicture())) {
			return nextRepository.getLink(profile, format);
		}
		String picture = profile.getPicture();
		if (isImgurLink(picture)) {
			return picture;
		}
		return ScooldUtils.getDefaultAvatar();
	}

	private boolean isImgurLink(String picture) {
		return StringUtils.startsWithIgnoreCase(picture, "https://i.imgur.com/");
	}

	@Override
	public String getAnonymizedLink(String data) {
		return nextRepository.getAnonymizedLink(data);
	}

	@Override
	public boolean store(Profile profile, String url) {
		if (!isImgurLink(url)) {
			return nextRepository.store(profile, url);
		}
		profile.setPicture(url);
		return true;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import jakarta.inject.Singleton;

@Component
@Singleton
public class GravatarAvatarGenerator {
	private static final String URL_BASE = "https://www.gravatar.com/avatar/";

	public GravatarAvatarGenerator() {
	}

	public String getLink(Profile profile, AvatarFormat format) {
		return configureLink(getRawLink(profile), format);
	}

	public String getRawLink(Profile profile) {
		String email = (profile == null || profile.getUser() == null) ? "" : profile.getUser().getEmail();
		return getRawLink(email);
	}

	public String getRawLink(String email) {
		return URL_BASE + computeToken(email);
	}

	private String computeToken(String email) {
		if (StringUtils.isBlank(email)) {
			return "";
		}

		return Utils.md5(email.toLowerCase());
	}

	public String configureLink(String url, AvatarFormat format) {
		return url + (url.endsWith("?") ? "&" : "?") + "s=" + format.getSize() + "&r=g&d=" + ScooldUtils.gravatarPattern();
	}

	public boolean isLink(String link) {
		return StringUtils.contains(link, "gravatar.com");
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.scoold.core.Profile;
import org.apache.commons.lang3.StringUtils;

public class GravatarAvatarRepository implements AvatarRepository {
	private final GravatarAvatarGenerator gravatarAvatarGenerator;
	private final AvatarRepository nextRepository;

	public GravatarAvatarRepository(GravatarAvatarGenerator gravatarAvatarGenerator, AvatarRepository nextRepository) {
		this.gravatarAvatarGenerator = gravatarAvatarGenerator;
		this.nextRepository = nextRepository;
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		if (profile == null) {
			return nextRepository.getLink(profile, format);
		}

		String picture = profile.getPicture();
		if (StringUtils.isBlank(picture)) {
			return gravatarAvatarGenerator.getLink(profile, format);
		}

		if (!gravatarAvatarGenerator.isLink(picture)) {
			return nextRepository.getLink(profile, format);
		}

		return gravatarAvatarGenerator.configureLink(picture, format);
	}

	@Override
	public String getAnonymizedLink(String data) {
		return gravatarAvatarGenerator.getRawLink(data);
	}

	@Override
	public boolean store(Profile profile, String url) {
		if (StringUtils.isBlank(url)) {
			String gravatarUrl = gravatarAvatarGenerator.getRawLink(profile);
			return applyChange(profile, gravatarUrl);
		}

		if (!gravatarAvatarGenerator.isLink(url)) {
			return nextRepository.store(profile, url);
		}

		return applyChange(profile, url);
	}

	private boolean applyChange(Profile profile, String url) {
		profile.setPicture(url);
		return true;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import org.springframework.stereotype.Component;
import jakarta.inject.Singleton;
import software.amazon.awssdk.utils.StringUtils;

@Component
@Singleton
public class AvatarRepositoryProxy implements AvatarRepository {
	private final AvatarRepository repository;

	public AvatarRepositoryProxy(GravatarAvatarGenerator gravatarAvatarGenerator) {
		this.repository = addGravatarIfEnabled(addCloudinaryIfEnabled(addImgurIfEnabled(getDefault())), gravatarAvatarGenerator);
	}

	private AvatarRepository addGravatarIfEnabled(AvatarRepository repo, GravatarAvatarGenerator gravatarAvatarGenerator) {
		return ScooldUtils.isGravatarEnabled() ? new GravatarAvatarRepository(gravatarAvatarGenerator, repo) : repo;
	}

	private AvatarRepository addImgurIfEnabled(AvatarRepository repo) {
		return ScooldUtils.isImgurAvatarRepositoryEnabled() ? new ImgurAvatarRepository(repo) : repo;
	}

	private AvatarRepository addCloudinaryIfEnabled(AvatarRepository repo) {
		return ScooldUtils.isCloudinaryAvatarRepositoryEnabled() ? new CloudinaryAvatarRepository(repo) : repo;
	}

	private AvatarRepository getDefault() {
		return new DefaultAvatarRepository();
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		return repository.getLink(profile, format);
	}

	@Override
	public String getAnonymizedLink(String data) {
		return repository.getAnonymizedLink(data);
	}

	@Override
	public boolean store(Profile profile, String url) {
		if (profile != null && StringUtils.equals(profile.getPicture(), url)) {
			return false;
		}
		return repository.store(profile, url);
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

public enum AvatarFormat {
	Square25(25),
	Square32(32),
	Square50(50),
	Square127(127),
	Profile(404);

	private final int size;

	AvatarFormat(int size) {
		this.size = size;
	}

	public int getSize() {
		return this.size;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import org.apache.commons.lang3.StringUtils;

public class DefaultAvatarRepository implements AvatarRepository {

	public DefaultAvatarRepository() {
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		return (profile == null || StringUtils.isBlank(profile.getPicture())) ? ScooldUtils.getDefaultAvatar() : profile.getPicture();
	}

	@Override
	public String getAnonymizedLink(String data) {
		return ScooldUtils.getDefaultAvatar();
	}

	@Override
	public boolean store(Profile profile, String url) {
		if (StringUtils.isBlank(url) || !url.equalsIgnoreCase(profile.getOriginalPicture())) {
			if (StringUtils.startsWithIgnoreCase(url, ScooldUtils.getConfig().serverUrl())) {
				profile.setPicture(url);
			} else {
				profile.setPicture(ScooldUtils.getDefaultAvatar());
			}
		} else {
			profile.setPicture(profile.getOriginalPicture());
		}
		return true;
	}
}

/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
 *
 * For issues and patches go to: https://github.com/erudika
 */

import com.erudika.scoold.core.Profile;
import org.apache.commons.lang3.StringUtils;

public class CloudinaryAvatarRepository implements AvatarRepository {
	private static final String BASE_URL = "https://res.cloudinary.com/";

	private final AvatarRepository nextRepository;

	public CloudinaryAvatarRepository(AvatarRepository nextRepository) {
		this.nextRepository = nextRepository;
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		if (profile == null) {
			return nextRepository.getLink(profile, format);
		}

		String picture = profile.getPicture();
		if (!isCloudinaryLink(picture)) {
			return nextRepository.getLink(profile, format);
		}

		return configureLink(picture, format);
	}

	@Override
	public String getAnonymizedLink(String data) {
		return nextRepository.getAnonymizedLink(data);
	}

	@Override
	public boolean store(Profile profile, String url) {
		if (!isCloudinaryLink(url)) {
			return nextRepository.store(profile, url);
		}

		return applyChange(profile, url);
	}

	private boolean applyChange(Profile profile, String url) {
		profile.setPicture(url);

		return true;
	}

	private boolean isCloudinaryLink(String url) {
		return StringUtils.startsWith(url, BASE_URL);
	}

	private String configureLink(String url, AvatarFormat format) {
		return url.replace("/upload/", "/upload/t_" + getTransformName(format) + "/");
	}

	private String getTransformName(AvatarFormat format) {
		return format.name().toLowerCase();
	}
}

