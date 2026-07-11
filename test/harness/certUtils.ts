/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import tls from "tls";

import forge from "node-forge";

export interface CaData {
  certPem: string;
  keyPem: string;
  caCert: forge.pki.Certificate;
  caKey: forge.pki.rsa.PrivateKey;
}

export function generateCA(): CaData {
  const keys = forge.pki.rsa.generateKeyPair(2048);
  const cert = forge.pki.createCertificate();
  cert.publicKey = keys.publicKey;
  cert.serialNumber = "01";

  const now = new Date();
  const oneYearLater = new Date();
  oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);
  cert.validity.notBefore = now;
  cert.validity.notAfter = oneYearLater;

  const attrs: forge.pki.CertificateField[] = [
    { name: "commonName", value: "SDK E2E Test CA" },
    { name: "organizationName", value: "Copilot SDK Tests" },
  ];
  cert.setSubject(attrs);
  cert.setIssuer(attrs);

  cert.setExtensions([
    { name: "basicConstraints", cA: true, critical: true },
    { name: "keyUsage", keyCertSign: true, cRLSign: true, critical: true },
  ]);

  cert.sign(keys.privateKey, forge.md.sha256.create());

  return {
    certPem: forge.pki.certificateToPem(cert),
    keyPem: forge.pki.privateKeyToPem(keys.privateKey),
    caCert: cert,
    caKey: keys.privateKey,
  };
}

export function createSecureContextForHost(
  hostname: string,
  ca: CaData,
): tls.SecureContext {
  const keys = forge.pki.rsa.generateKeyPair(2048);
  const cert = forge.pki.createCertificate();
  cert.publicKey = keys.publicKey;
  cert.serialNumber = String(Date.now());

  const now = new Date();
  const oneYearLater = new Date();
  oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);
  cert.validity.notBefore = now;
  cert.validity.notAfter = oneYearLater;

  cert.setSubject([{ name: "commonName", value: hostname }]);
  cert.setIssuer(ca.caCert.subject.attributes);
  cert.setExtensions([
    {
      name: "subjectAltName",
      altNames: [{ type: 2, value: hostname }],
    },
  ]);

  cert.sign(ca.caKey, forge.md.sha256.create());

  return tls.createSecureContext({
    key: forge.pki.privateKeyToPem(keys.privateKey),
    cert: forge.pki.certificateToPem(cert),
    ca: ca.certPem,
  });
}
