Overview - A Shared Platform for Development and Testing
The purpose of this platform is to provide a readily available IDP or SP for your development and integration testing. Please don't upload any PRODUCTION information. Furthermore, SAML Entity information uploaded is identified by its Entity ID. Hence, to avoid being overwritten by others, best practice is to choose a unique SAML Entity ID when uploading metadata.

SAML BOX has two components SP (Service Provider) and IDP (Identity Provider).

For SP, Spring Security SAML 6.4.3 (https://docs.spring.io/spring-security/reference/servlet/saml2/index.html) is used to handle various parts of  SAML flow when SAML BOX is acting as SP. This includes generating and sending SAML authentication request, customizing that request, validating SAML response from IDP, extract claims and etc.

For IDP, Spring Framework doesn't support it yet, so an alterantive is used here, and that is Keycloak (https://www.keycloak.org/). The version used here is 26.1.2. Keycloak is a centralized Identity and Access Management platform that supports various protocols like OAuth, OIDC, SAML and etc. When working with SAML, it's mainly serving as an IDP, integrating with other SPs for SSO flows.

Platform Architecture
Here is a diagram of SAML BOX architecture.
<architecture-diagram>


SAML BOX Metadata
For SAML flow to work, IDP and SP need to do handshake by exchanging metadata information so that they know who to trust. Both SP and IDP metadata can be downloaded from SAML box. Correspondingly, the other party's metadata needs to be uploaded to SAML BOX too.



****
SAML BOX as SP
There are two types of workflows in SAML: SP-Initiated SSO and IDP-Initiated SSO. (If you are interested in learning more details, you can check out the tutorial blogs here https://www.bostonidentity.com/post/saml-2-0-explained-in-simple-words-part-i). In SP-Init SSO, a SAML AuthnRequest is sent to IDP and IDP will generate a response after validating a user and send back to SP. For that SAML AuthnRequest, there are various things we can configure to tweak the AuthnReqest so that IDP can respond differently based on the ask. Here are some of them that you can play with in SAML BOX.


SAML Request Bindings
In SAML, bindings are methods used to transport SAML messages between IDP and SP. There are several bindings defined in the SAML specification:
- HTTP POST Binding
- HTTP Redirect Binding
- HTTP Artifact Binding
- SOAP Binding

HTTP POST is the most used while HTTP Redirect is still seen. Artifact and SOAP bindings are legacy ones and are not that popular.
SAML BOX supports switching between HTTP POST and HTTP Redirect. Note that this is regarding the SAML Authentication Request Binding, which is the request that sent from SP to IDP, not SAML Response which is sent from IDP to SP.


NameID-Format
NameID-Format depicts how the subject value (the value that identifies the user authenticated to IDP) returned in the SAML response should be interpreted by SP. For example, if it's shown as 'emailAddress', the subject value should be interpreted as an email value.

Here are some NameID-Format values:
- urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified
- urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress
- urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
- urn:oasis:names:tc:SAML:2.0:nameid-format:transient
- urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
- urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos
- urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName

SAML BOX supports selecting one of them or type in custom format value.


SAML Request Signature
The signature information is not required in the SAML Request, but is good to have. In this case, IDP can verify the signature using the Public Cert provided (e.g. in SP Metadata) to make sure the request is not tampered.

You can specify whether SAML BOX should add a signature when sending the SAML request.


SAML BOX SP Session
When a SAML response is returned, SAML BOX will extract the assertions and generate a user session (Spring Security Session). You can view this session information from SAML BOX UI, which will be helpful for development/testing. Note that a new SAML response will have a new session overriding an existing session.


SP-Initialized Single Logout (SLO)
Single Logout allows a user to log out from all sessions across multiple applications (SPs) plus IDP itself in a single action. In this case, a user doesn't need to manually logout each app.

SAML Box supports SP-init Single Logout, where a SAML Single Logout request is sent to IDP. Note that it's IDP's responsibility to process that request and send logout response to other SPs. Here for SAML BOX, when a Single Logout response is sent back, it will validate and process it. Once the logout is done, the SAML BOX session will be killed, which can be seen from the session info section in SAML BOX UI.

Another note is that IDP-init single logout (unsolicited single logout, similar to IDP-init single sign-on) may not be supported here due to the limitation of Keycloak.


SAML Response Encryption
While SAML response signature is required, SAML response encryption is optional. The former protects the data integrity of the response and the latter protects the data confidentiality of the response. When security level is high, SAML encryption is usually needed, and signature is usually required too. Note that IDP usually signs first and then encrypt.

When SAML BOX acts as SP, it supports SAML response encryption. A signature is required when encryption is enabled. IDP uses SP's public cert to encrypt the response and SP will decrypt it using its private key.


****
SAML BOX as IDP
As Spring Security SAML is not supporting IDP yet, Keycloak is used here as IDP provider.

Keycloak is an Open Source Identity and Access Management platform. It adds authentication to applications and secure services with minimum effort and it provides user federation, strong authentication, user management, fine-grained authorization, and etc.

SAML BOX leverages Keycloak APIs to fulfill the IDP role. When a SAML SP client is registered (Keycloak calls SP as client, same as in OAuth/OIDC), a list of settings is available for this specific SP. Note that since Keycloak is a centralized IAM platform, it can register multiple SPs and each SP will have its own specific settings for the login flow.


IDP-Initiated SSO
Normally an IDP generates a SAML response based on the incoming SAML request. Occassionaly, IDP can generate an unsolicited SAML response and directly send to SP, and this is called IDP-Initiated SSO. This flow is useful when SP can't generate SAML request but can accept SAML response.

Note that When using IDP-Initiated SSO, a URL endpoint needs to be specified in the registered SP in Keycloak, which is supported by SAML BOX as well.


SAML Response Bindings (ACS)
Similar to SAML Request Bindings, the different binding types can be used in the SAML response too. SAML BOX (Keycloak) supports both HTTP POST and HTTP Redirect, which can be specified in the SAML BOX settings.


NameID-Format
Some NameID-formats are supported by Keycloak:
- urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified
- urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress
- urn:oasis:names:tc:SAML:2.0:nameid-format:persistent

When a name NameID-format is selected, that seleced one will be the format returned in the SAML response.


SAML Response Signature
The signature for SAML Response can be at either Assertion level or the entire Message level or both (check here for more info https://www.bostonidentity.com/post/saml-2-0-explained-in-simple-words-part-ii).


SAML Response Encryption
SAML BOX supports encryption when acting as an IDP. Note that an encryption public cert is required when uploading SP metadata.


