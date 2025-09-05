package com.datafit.migrater.service;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;

/**
 * Simplified SftpService snippet showing host-key verification via OpenSSHKnownHosts (StringReader)
 * and a PromiscuousVerifier fallback for dev.
 */
public class SftpService {

    public SFTPClient connectAndCreateSftp(String host, int port, String user, String password, String knownHostsContent) throws Exception {
        SSHClient ssh = new SSHClient();

        // Configure host key verifier:
        if (knownHostsContent != null && !knownHostsContent.isBlank()) {
            // Use StringReader so we don't need to write temp files
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(new StringReader(knownHostsContent)));
        } else {
            // Development fallback: accepts any host key (INSECURE; do not use in production)
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
        }

        ssh.connect(host, port);
        try {
            ssh.authPassword(user, password);
            return ssh.newSFTPClient();
        } catch (Exception ex) {
            ssh.disconnect();
            throw ex;
        }
    }


    // other SFTP utility methods...
}