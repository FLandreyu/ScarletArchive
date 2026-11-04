package cn.scarlet.service;

import cn.scarlet.api.Problem;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssetService {

  private final Path root;

  public AssetService(@Value("${scarlet.uploads}") String root) {
    this.root = Path.of(root).toAbsolutePath().normalize();
  }

  public Map<String, Object> upload(MultipartFile file) throws Exception {
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw new Problem(
      400,
      "UPLOAD_SIZE",
      "请选择5MB以内的图片"
    );
    if (
      !Set.of("image/png", "image/jpeg").contains(Objects.requireNonNullElse(file.getContentType(), ""))
    ) throw new Problem(400, "UPLOAD_TYPE", "只支持 PNG 或 JPEG 位图，不接受 SVG");
    BufferedImage image;
    try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw new Problem(400, "UPLOAD_TYPE", "文件内容不是有效图片");
      var reader = readers.next();
      try {
        reader.setInput(input, true, true);
        String format = reader.getFormatName();
        if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) throw new Problem(
          400,
          "UPLOAD_TYPE",
          "图片格式与允许类型不符"
        );
        int w = reader.getWidth(0),
          h = reader.getHeight(0);
        if (w < 1 || h < 1 || (long) w * h > 16000000) throw new Problem(
          400,
          "UPLOAD_PIXELS",
          "图片不得超过1600万像素"
        );
        image = reader.read(0);
      } finally {
        reader.dispose();
      }
    }
    int width = Math.min(1920, image.getWidth()),
      height = Math.max(1, (int) (((double) image.getHeight() * width) / image.getWidth()));
    BufferedImage safe = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = safe.createGraphics();
    g.setColor(java.awt.Color.WHITE);
    g.fillRect(0, 0, width, height);
    g.drawImage(image, 0, 0, width, height, null);
    g.dispose();
    Files.createDirectories(root);
    String name = UUID.randomUUID() + ".jpg";
    Path target = root.resolve(name);
    if (!target.startsWith(root)) throw new IllegalStateException();
    ImageIO.write(safe, "jpeg", target.toFile());
    return Map.of("url", "/uploads/" + name, "width", width, "height", height);
  }
}
