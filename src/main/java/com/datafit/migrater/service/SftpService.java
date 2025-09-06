package com.datafit.migrater.service;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.*;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.stereotype.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Simplified SftpService snippet showing host-key verification via OpenSSHKnownHosts (StringReader)
 * and a PromiscuousVerifier fallback for dev.
 */
@Service
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

    /**
     * Download remote files to local directory matching a simple wildcard pattern.
     *
     * Signature must exactly match how JobRunnerService calls it:
     * fetchToLocal(String host,int port,String user,String password,String remoteDir, Path localDir, String filePattern, Path knownHostsFile)
     */
    public List<Path> fetchToLocal(String host,
                                   int port,
                                   String user,
                                   String password,
                                   String remoteDir,
                                   Path localDir,
                                   String filePattern,
                                   Path knownHostsFile) throws Exception {

        if (localDir == null) throw new IllegalArgumentException("localDir required");
        Files.createDirectories(localDir);

        // convert simple wildcard into regex (very small utility)
        String regex = (filePattern == null || filePattern.isBlank()) ? ".*" :
                filePattern.trim().replace(".", "\\.").replace("*", ".*");
        Pattern p = Pattern.compile(regex);

        SSHClient ssh = new SSHClient();
        if (knownHostsFile != null && Files.exists(knownHostsFile)) {
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(knownHostsFile.toFile()));
        } else {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
        }

        List<Path> downloaded = new ArrayList<>();
        try {
            ssh.connect(host, port);
            try {
                if (password != null) ssh.authPassword(user, password);
                else ssh.authPublickey(user);
                try (SFTPClient sftp = ssh.newSFTPClient()) {
                    List<RemoteResourceInfo> remoteFiles = sftp.ls(remoteDir).stream()
                            .filter(RemoteResourceInfo::isRegularFile)
                            .collect(Collectors.toList());
                    for (RemoteResourceInfo r : remoteFiles) {
                        String name = r.getName();
                        if (!p.matcher(name).matches()) continue;
                        Path out = localDir.resolve(name);
                        sftp.get(r.getPath(), out.toString());
                        downloaded.add(out);
                    }
                }
            } finally {
                ssh.disconnect();
            }
        } catch (Exception ex) {
            throw ex;
        }
        return downloaded;
    }
}