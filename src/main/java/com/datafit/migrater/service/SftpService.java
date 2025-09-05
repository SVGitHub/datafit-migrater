package com.datafit.migrater.service;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SFTP helper - supports password and key-based auth, and optionally known_hosts verification.
 * Also supports streaming individual remote files directly to S3 via an InputStream.
 */
@Service
public class SftpService {

    public List<Path> fetchToLocal(String host, int port, String user, String password, String remotePath, Path localDir, String glob, Path knownHostsFile) throws IOException {
        SSHClient ssh = new SSHClient();
        if(knownHostsFile!=null && Files.exists(knownHostsFile)) {
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(knownHostsFile.toString()));
        } else {
            ssh.addHostKeyVerifier((h, p, key) -> true); // permissive - replace in prod
        }
        ssh.connect(host, port);
        try {
            ssh.authPassword(user, password);
            SFTPClient sftp = ssh.newSFTPClient();
            try {
                return downloadFromSftp(sftp, remotePath, localDir, glob);
            } finally {
                sftp.close();
            }
        } finally {
            ssh.disconnect();
            ssh.close();
        }
    }

    public List<Path> fetchToLocalWithKey(String host, int port, String user, Path privateKeyFile, String remotePath, Path localDir, String glob, Path knownHostsFile) throws IOException {
        SSHClient ssh = new SSHClient();
        if(knownHostsFile!=null && Files.exists(knownHostsFile)) {
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(knownHostsFile.toString()));
        } else {
            ssh.addHostKeyVerifier((h, p, key) -> true);
        }
        ssh.connect(host, port);
        try {
            ssh.authPublickey(user, privateKeyFile.toString());
            SFTPClient sftp = ssh.newSFTPClient();
            try {
                return downloadFromSftp(sftp, remotePath, localDir, glob);
            } finally {
                sftp.close();
            }
        } finally {
            ssh.disconnect();
            ssh.close();
        }
    }

    public String streamRemoteFileToS3(String host, int port, String user, String password, String remoteFilePath, com.datafit.migrater.service.S3Service s3Service, String bucket, String key, Path knownHostsFile) throws IOException {
        SSHClient ssh = new SSHClient();
        if(knownHostsFile!=null && Files.exists(knownHostsFile)) {
            ssh.addHostKeyVerifier(new OpenSSHKnownHosts(knownHostsFile.toString()));
        } else {
            ssh.addHostKeyVerifier((h, p, keyh) -> true);
        }
        ssh.connect(host, port);
        try {
            ssh.authPassword(user, password);
            SFTPClient sftp = ssh.newSFTPClient();
            try (InputStream is = sftp.read(remoteFilePath)) {
                long size = -1;
                return s3Service.uploadStream(bucket, key, is, size);
            } finally {
                sftp.close();
            }
        } finally {
            ssh.disconnect();
            ssh.close();
        }
    }

    private List<Path> downloadFromSftp(SFTPClient sftp, String remotePath, Path localDir, String glob) throws IOException {
        List<Path> downloaded = new ArrayList<>();
        Files.createDirectories(localDir);
        var entries = sftp.ls(remotePath);
        for (var e : entries) {
            String name = e.getName();
            if (name.equals(".") || name.equals("..")) continue;
            if (glob == null || glob.isBlank() || name.matches(globToRegex(glob))) {
                Path local = localDir.resolve(name);
                try (InputStream is = sftp.read(remotePath + "/" + name)) {
                    Files.copy(is, local, StandardCopyOption.REPLACE_EXISTING);
                    downloaded.add(local);
                }
            }
        }
        return downloaded;
    }

    private String globToRegex(String glob){
        return glob.replace(".", "\\.").replace("*", ".*");
    }
}
