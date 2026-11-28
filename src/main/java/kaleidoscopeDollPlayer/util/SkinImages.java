package kaleidoscopeDollPlayer.util;

import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class SkinImages {
    public static final int MAX_URL_LENGTH = 2048;
    // Two base64 layers must fit the vanilla profile property's 32767-character limit.
    public static final int MAX_SNAPSHOT_BYTES = 17_000;
    private static final int MAX_DOWNLOAD_BYTES = 256 * 1024;
    private static final Pattern MOJANG_PATH = Pattern.compile("/texture/[a-fA-F0-9]{32,64}");
    private static final ThreadPoolExecutor DOWNLOADS = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), Thread.ofPlatform().daemon().name("doll-snapshot-", 0).factory());

    private SkinImages() {}

    public static URI parseUserUrl(String text) {
        if (text.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Skin URL is too long");
        }
        URI uri = URI.create(text);
        String scheme = uri.getScheme();
        if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null || uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("Expected an HTTP(S) skin URL");
        }
        return uri;
    }

    public static URI mojangUrl(String text) {
        URI uri = parseUserUrl(text);
        if (!"textures.minecraft.net".equalsIgnoreCase(uri.getHost())
                || uri.getPort() != -1 || uri.getRawQuery() != null || uri.getRawFragment() != null
                || !MOJANG_PATH.matcher(uri.getRawPath()).matches()) {
            throw new IllegalArgumentException("Not a Mojang texture URL");
        }
        return URI.create("https://textures.minecraft.net" + uri.getRawPath().toLowerCase(Locale.ROOT));
    }

    public static CompletableFuture<byte[]> download(URI uri) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return fetch(uri);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }, DOWNLOADS);
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    static byte[] fetch(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(5_000);
        connection.setRequestProperty("User-Agent", KaleidoscopeDollPlayer.MOD_ID);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK
                    || connection.getContentLengthLong() > MAX_DOWNLOAD_BYTES) {
                throw new IOException("Skin download rejected");
            }
            try (var input = connection.getInputStream(); var bytes = new ByteArrayOutputStream()) {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (bytes.size() + count > MAX_DOWNLOAD_BYTES || System.nanoTime() - deadline >= 0) {
                        throw new IOException("Skin download exceeded its limits");
                    }
                    bytes.write(buffer, 0, count);
                }
                return normalize(bytes.toByteArray());
            }
        } finally {
            connection.disconnect();
        }
    }

    public static byte[] normalize(byte[] source) throws IOException {
        if (source.length > MAX_DOWNLOAD_BYTES) {
            throw new IOException("Skin PNG is too large");
        }
        BufferedImage image = decode(source);
        BufferedImage rgba = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int height = image.getHeight();
        rgba.setRGB(0, 0, 64, height, image.getRGB(0, 0, 64, height, null, 0, 64), 0, 64);
        if (height == 32) {
            expandLegacyLimbs(image, rgba);
        }
        var bytes = new ByteArrayOutputStream();
        var writer = ImageIO.getImageWritersByFormatName("png").next();
        try (var output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(rgba, null, null), writer.getDefaultWriteParam());
        } finally {
            writer.dispose();
        }
        if (bytes.size() > MAX_SNAPSHOT_BYTES) {
            throw new IOException("Encoded skin is too large");
        }
        return bytes.toByteArray();
    }

    static boolean hasSkinHeader(byte[] png) {
        if (png.length < 24) {
            return false;
        }
        var header = ByteBuffer.wrap(png);
        int height = header.getInt(20);
        return header.getLong() == 0x89504e470d0a1a0aL && header.getInt() == 13
                && header.getInt() == 0x49484452 && header.getInt() == 64 && (height == 64 || height == 32);
    }

    private static void expandLegacyLimbs(BufferedImage source, BufferedImage target) {
        // Legacy skins reuse the right limbs. Mirror the faces and swap the inner/outer sides.
        for (int limb = 0; limb < 2; limb++) {
            int from = limb == 0 ? 0 : 40;
            int to = limb == 0 ? 16 : 32;
            mirrorLegacyFace(source, target, from + 4, 16, to + 4, 48, 4);
            mirrorLegacyFace(source, target, from + 8, 16, to + 8, 48, 4);
            for (int face = 0; face < 16; face += 4) {
                int mirrored = face == 0 ? 8 : face == 8 ? 0 : face;
                mirrorLegacyFace(source, target, from + mirrored, 20, to + face, 52, 12);
            }
        }
    }

    private static void mirrorLegacyFace(BufferedImage source, BufferedImage target, int sx, int sy, int tx, int ty, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < 4; x++) {
                target.setRGB(tx + x, ty + y, source.getRGB(sx + 3 - x, sy + y));
            }
        }
    }

    private static BufferedImage decode(byte[] source) throws IOException {
        if (!hasSkinHeader(source)) {
            throw new IOException("Expected a 64x64 or 64x32 skin PNG");
        }
        var reader = ImageIO.getImageReadersByFormatName("png").next();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(source))) {
            reader.setInput(input, true, true);
            return reader.read(0);
        } catch (RuntimeException e) {
            throw new IOException("Invalid skin PNG", e);
        } finally {
            reader.dispose();
        }
    }
}
