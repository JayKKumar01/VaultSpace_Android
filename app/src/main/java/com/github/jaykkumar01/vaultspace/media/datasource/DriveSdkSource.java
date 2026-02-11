//package com.github.jaykkumar01.vaultspace.media.datasource;
//
//import com.google.api.services.drive.Drive;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//final class DriveSdkSource implements DriveStreamSource {
//
//    private final Drive drive;
//    private final String fileId;
//
//    @Override
//    public StreamSession open(long position) throws IOException {
//
//        Drive.Files.Get req = drive.files().get(fileId);
//        req.getMediaHttpDownloader().setDirectDownloadEnabled(true);
//        req.setDisableGZipContent(true);
//
//        if (position > 0)
//            req.getRequestHeaders().setRange("bytes=" + position + "-");
//
//        InputStream stream = req.executeMediaAsInputStream();
//
//        return new StreamSession() {
//            @Override public InputStream stream() { return stream; }
//
//            @Override
//            public void cancel() {
//                try { stream.close(); } catch (Exception ignored) {}
//            }
//        };
//    }
//}
