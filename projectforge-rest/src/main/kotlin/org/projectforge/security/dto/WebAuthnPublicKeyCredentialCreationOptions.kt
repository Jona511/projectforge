/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.security.dto

import com.webauthn4j.data.AuthenticatorSelectionCriteria
import com.webauthn4j.data.PublicKeyCredentialParameters

/**
 * Doc, please refer [Yubico developer guide](https://developers.yubico.com/WebAuthn/WebAuthn_Developer_Guide/WebAuthn_Client_Registration.html)
 */
class WebAuthnPublicKeyCredentialCreationOptions(
  /**
   * Yubico: *relying party information. name is required. The id attribute must equal the domain of the origin seen by
   * the client, or the origin must be a subdomain of the id. If id is omitted, then origins effective domain is used.*
   */
  var rp: WebAuthnRp,
  var user: WebAuthnUser,
  /**
   * Yubico: *In order to prevent replay attacks, the value should be randomly generated by the RP.*
   */
  var challenge: String,
  /**
   * Yubico: *the time, in milliseconds, that the caller is willing to wait for the call to complete.*
   */
  var timeout: Long,
  var requestId: String,
  var sessionToken: String,
  /**
   * Yubico: *Specifies which cryptographic algorithms the RP supports. type: only one type: “public-key”. alg:
   * cryptographic signature algorithm preference specified by COSE Algorithms registry. If you use a library to
   * validate authenticator responses, pubKeyCredParams is probably determined by what the library suppports.*
   */
  var pubKeyCredParams: Array<PublicKeyCredentialParameters>,
  /**
   *  Yubico: *specify authenticator requirements. The optional authenticatorAttachment attribute filters eligible
   *  authenticator by type. The value “platform” indicates a platform authenticator, such as Windows Hello. The value
   *  "cross-platform" value indicates a roaming authenticator, such as a security key. When the requireResidentKey
   *  attribute is true, the authenticator must create a discoverable credential (a.k.a. resident key). The
   *  userVerification attribute can be set to a value of “preferred”, “required”, or “discouraged”.*
   */
  var authenticatorSelection: AuthenticatorSelectionCriteria,
  /**
   * Yubico: *The default value is “none” which means the RP is not interested in authenticator attestation.
   * “indirect” indicates the RP prefers a verifiable attestation statement but allows the client to decide
   * how to obtain it. “direct” indicates the RP wants to receive the attestation statement.
   * It is recommended that RPs use the “direct” value and store the attestation statement with the credential
   * so they can inspect authenticator models retroactively if policy changes.*
   */
  var attestation: String = "direct",
  var extensions: WebAuthnExtensions,
) {
  /**
   * excludeCredentials: limits creation of multiple credentials for the same account on a single authenticator.
   * If the authenticator finds an existing credential type and id for the RP, then a new credential will not be created.
   */
  var excludeCredentials: Array<WebAuthnCredential>? = null
}

/*
{
  "publicKey": {
    "rp": {
      "name": "Yubico WebAuthn demo",
      "id": "localhost"
    },
    "user": {
      "name": "kai",
      "displayName": "kai",
      "id": "jTNp7UFg4AmWu1-SdW2LJtwOglXnoVS4CItD1HHkGQU"
    },
    "challenge": "8InUMLpDVCMJZI0PrSbbQXjDWACGJm96XoHkr8_Ixis",
    "pubKeyCredParams": [
      {
        "alg": -7,
        "type": "public-key"
      },
      {
        "alg": -8,
        "type": "public-key"
      },
      {
        "alg": -257,
        "type": "public-key"
      }
    ],
    "excludeCredentials": [],
    "authenticatorSelection": {
      "requireResidentKey": false,
      "residentKey": "discouraged",
      "userVerification": "preferred"
    },
    "attestation": "direct",
    "extensions": {
      "appidExclude": "https://localhost:8443",
      "credProps": true
    }
  }
} */
