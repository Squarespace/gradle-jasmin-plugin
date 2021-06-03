package com.squarespace.gradle.jasmin;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

/**
 * Gradle plugin for compiling Jasmin assembly source code.
 */
public class JasminPlugin implements Plugin<Project> {

  public void apply(Project project) {
    System.setProperty("file.encoding", "UTF-8");

    // We piggy-back on the Java plugin so make sure it's applied
    project.getPlugins().apply("java");

    // Loop over the existing Java source sets (e.g. "main", "test")
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
      public void execute(SourceSet javaSourceSet) {

        // Setup naming of our source set
        final String name = javaSourceSet.getName();
        final String displayName = ((DefaultSourceSet)javaSourceSet).getDisplayName();

        // Add a "src/{main,test}/jasmin/**/*.j" source set
        final String sourcePath = Paths.get("src").resolve(name).resolve("jasmin").toString();
        final SourceDirectorySet jasminSet = project.getObjects().sourceDirectorySet(name, displayName);
        jasminSet.getFilter().include(new String[] { "**/*.j" });
        jasminSet.srcDir(sourcePath);

        // Ensure Jasmin sources are visible in the Java source set
        javaSourceSet.getAllJava().source(jasminSet);
        javaSourceSet.getAllSource().source(jasminSet);

        // Get references to the build and classes output directories
        final String buildDir = project.getLayout().getBuildDirectory().get().toString();
        final Path destinationDir = Paths.get(buildDir).resolve("classes").resolve("jasmin").resolve(name);

        // Create a named task for compiling Jasmin files in the current source set (e.g. "main")
        final String taskName = javaSourceSet.getCompileTaskName("jasmin");

        // Ensure compilation of Java depends on Jasmin compilation
        Task javaTask = project.getTasks().getByName(javaSourceSet.getCompileJavaTaskName());
        javaTask.dependsOn(taskName);

        // Ensure compiled Java classes can see Jasmin classes on the classpath
        javaSourceSet.setCompileClasspath(javaSourceSet.getCompileClasspath().plus(project.files(destinationDir)));

        // Ensure jar contains Jasmin-compiled classes
        DefaultSourceSetOutput outputSet = (DefaultSourceSetOutput) javaSourceSet.getOutput();
        outputSet.addClassesDir(project.getLayout().dir(project.provider(() -> destinationDir.toFile())));

        // Create the Jasmin compile task for this source set
        project.getTasks().create(taskName, JasminCompile.class, (task) -> {
          task.setDescription("Compiles the " + name + " Jasmin source");
          task.setSource(jasminSet);
          task.setClasspath(javaSourceSet.getCompileClasspath());
          task.setDestinationDir(destinationDir.toFile());
          task.sourceDirs(jasminSet.getSrcDirs());
        });

      }
    });
  }
}
