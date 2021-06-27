package com.squarespace.gradle.jasmin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;

import jasmin.ClassFile;


/**
 * Assembles Jasmin source into JVM classfile format.
 */
public class JasminCompile extends AbstractCompile {

  private List<Path> sourceDirs;

  public void sourceDirs(Set<File> sourceDirs) {
    this.sourceDirs = sourceDirs.stream()
        .map(e -> Paths.get(e.getAbsolutePath()))
        .collect(Collectors.toList());
  }

  static class JasminClassLoader extends ClassLoader {
    private final String name;
    private final byte[] data;
    public JasminClassLoader(String name, byte[] data) {
      super();
      this.name = name;
      this.data = data;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      if (this.name.equals(name)) {
        return defineClass(name, data, 0, data.length);
      }
      return super.loadClass(name);
    }
  }

  @TaskAction
  public void compile() {
    Path destDir = Paths.get(getDestinationDir().getAbsolutePath());
    for (File file : getSource().getFiles()) {

      // Read source into a Jasmin class file object
      ClassFile cls = new ClassFile();
      try (InputStream is = new FileInputStream(file)) {
        cls.readJasmin(is, file.getName(), true);
      } catch (Exception e) {
        throw new GradleException("Error reading Jasmin input file", e);
      }

      // Construct output path. We use the relative path since we cannot
      // obtain the package name from the Jasmin class file directly
      Path sourcePath = relativeSourcePath(file);
      String baseName = baseName(file.getName());
      String fqName = sourcePath.resolve(baseName).toString().replace('/', '.');

      // Trigger the Java class verification pass before writing to disk.
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try {
        cls.write(out);
        JasminClassLoader loader = new JasminClassLoader(fqName, out.toByteArray());
        Class<?> clazz = loader.loadClass(fqName.replace('/', '.'));
        clazz.getMethods();
      } catch (Exception e) {
        throw new GradleException("Error loading class file", e);
      }

      Path outDir = destDir.resolve(sourcePath);
      makedirs(outDir);
      String outPath = outDir.resolve(baseName + ".class").toString();

      // Write class file
      try (OutputStream os = new FileOutputStream(outPath)) {
        cls.write(os);
      } catch (Exception e) {
        throw new GradleException("Error writing class output file", e);
      }

    }
  }

  /**
   * Ensure the given directory and all of its parents exist or are created.
   */
  private void makedirs(Path dir) {
    File file = dir.toFile();
    if (!file.exists()) {
      file.mkdirs();
    }
  }

  /**
   * Return the relative path to the file under one of the source directories.
   */
  private Path relativeSourcePath(File file) {
    Path filePath = Paths.get(file.getAbsolutePath());
    for (Path dir : sourceDirs) {
       if (filePath.startsWith(dir)) {
         Path relativePath = dir.relativize(filePath).getParent();
         if (relativePath == null) {
           // The source file is in the default package.
           return Path.of("");
         }
         return relativePath;
       }
    }
    return filePath.getParent();
  }

  /**
   * Strip the file extension, if any.
   */
  private String baseName(String name) {
    int i = name.lastIndexOf('.');
    return i == -1 ? name : name.substring(0, i);
  }
}

