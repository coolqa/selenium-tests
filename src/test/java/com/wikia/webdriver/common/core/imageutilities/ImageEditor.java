package com.wikia.webdriver.common.core.imageutilities;

import com.wikia.webdriver.common.core.configuration.Configuration;
import com.wikia.webdriver.common.core.helpers.Emulator;
import com.wikia.webdriver.common.logging.PageObjectLogging;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriverException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageEditor {

  private int dpr;

  public ImageEditor() {
    this.dpr = getDevicePixelRatio();
  }

  public void saveImageFile(File imageFile, String path) {
    Pattern pattern = Pattern.compile("/*.jpg|/*.png|/*.jpeg");
    Matcher matcher = pattern.matcher(path);
    String newPath = null;
    if (!matcher.matches()) {
      newPath = path + ".png";
    }
    try {
      FileUtils.copyFile(imageFile, new File(newPath));
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
  }

  public File cropImage(org.openqa.selenium.Point start, org.openqa.selenium.Dimension size,
                        BufferedImage image) {
    int width = size.width;
    int height = size.height;
    File subImg;
    try {
      subImg = File.createTempFile("screenshot", ".png");
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
    if (width < 1) {
      width = 1;
    }
    if (height < 1) {
      height = 1;
    }
    // workaround for whenever WebElement.getLocation() return negatives
    // https://github.com/Wikia/selenium-tests/blob/9d00f02ba24391534a80520908c2973d9e6bed86/src/test/java/com/wikia/webdriver/common/core/imageutilities/Shooter.java#L46
    if (start.getX() < 0) {
      start.move(0, start.getY());
    }
    if (start.getY() < 0) {
      start.move(start.getX(), 0);
    }

    PageObjectLogging.log("Cropping screenshot, dpr = " + dpr, String.format("Cropping image %sx%s. Start position %sx%s, subimage %sx%s",
            image.getWidth(), image.getHeight(), start.getX() * dpr, start.getY() * dpr, width * dpr, height * dpr), true);
    BufferedImage dest = image.getSubimage(
        start.getX() * dpr, start.getY() * dpr, width * dpr, height * dpr
    );
    try {
      ImageIO.write(dest, "png", subImg);
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
    return subImg;
  }

  public File cropImage(Point start, org.openqa.selenium.Dimension size, File image) {
    BufferedImage img = fileToImage(image);
    return cropImage(start, size, img);
  }

  public BufferedImage fileToImage(File file) {
    BufferedImage img;
    try {
      img = ImageIO.read(file);
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
    return img;
  }

  /**
   * Gets device pixel ratio from emulator, 1 if not present. If no emulator is used dpr is taken from config.yaml
   * and should reflect your monitor dpr e.g. 2 for Macbook Pro
   * @return dpr
   */
  private static int getDevicePixelRatio() {
    if (Configuration.getEmulator() == Emulator.DEFAULT){
      if (Configuration.getDpr() == null)
        throw new ConfigurationRuntimeException("No dpr variable found in config.yaml");
      return Integer.parseInt(Configuration.getDpr());
    } else{
      Map<String, Object> metrics = Configuration.getEmulator().getDeviceMetrics();
      return (metrics != null && metrics.containsKey("pixelRatio")) ? ((Double) metrics.get("pixelRatio")).intValue() : 1;
    }
  }
}
