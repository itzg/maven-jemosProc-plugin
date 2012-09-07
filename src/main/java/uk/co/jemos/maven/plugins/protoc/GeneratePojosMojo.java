package uk.co.jemos.maven.plugins.protoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import uk.co.jemos.maven.plugins.utils.JemosProtocPluginUtils;

/**
 * Goal which triggers the generation of proto files from an XSD file
 * 
 * @goal generatePojos
 * 
 * @phase process-sources
 * 
 * @requiresDependencyResolution compile
 */
public class GeneratePojosMojo extends AbstractMojo {

	/**
	 * The current Maven project.
	 * 
	 * @parameter default-value="${project}"
	 * @readonly
	 * @required
	 */
	protected MavenProject project;

	/**
	 * A helper used to add resources to the project.
	 * 
	 * @component
	 * @required
	 */
	protected MavenProjectHelper projectHelper;

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${inputFolder}"
	 * @required
	 */
	private File inputFolder;

	/**
	 * The location where the generated files will be placed.
	 * 
	 * @parameter expression="${outputFolder}"
	 * @required
	 */
	private File outputFolder;

	/**
	 * The full path to the protoc compiler executable
	 * 
	 * @parameter expression="${protocExecutable}"
	 * 
	 */
	private String protocExecutable;

	public void execute() throws MojoExecutionException {

		if (protocExecutable == null) {
			protocExecutable = JemosProtocPluginUtils.PROTOC_EXECUTABLE;
		}

		if (!outputFolder.exists() || !outputFolder.isDirectory()) {
			getLog().info("Folder: " + outputFolder + " does not exist. Creating one...");
			outputFolder.mkdirs();
			getLog().info("Folder: " + outputFolder + " created.");
		}

		String outputFolderPath = outputFolder.getAbsolutePath().replace('\\', '/');

		String inputFolderPath = inputFolder.getAbsolutePath().replace('\\', '/');
		if (!inputFolderPath.endsWith("/")) {
			inputFolderPath += "/";
		}

		StringBuilder buff = new StringBuilder();
		buff.append("Protoc executable: " + protocExecutable + "\n");
		buff.append("Input Folder: " + inputFolderPath + "\n");
		buff.append("Output folder: " + outputFolderPath + "\n");

		getLog().info(buff.toString());

		List<String> cmdList = new ArrayList<String>();
		cmdList.add(protocExecutable);
		cmdList.add("--proto_path="+inputFolderPath);
		cmdList.add("--java_out="+outputFolderPath);
		File[] protoFiles = new File(inputFolderPath).listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".proto");
			}
		});
		for (File protoFile : protoFiles) {
			cmdList.add(protoFile.getPath());
		}
		
		getLog().debug("Will execute: " + cmdList);

		BufferedReader reader = null;

		try {

			Process p = Runtime.getRuntime().exec(cmdList.toArray(new String[]{}));
			int exitValue = p.waitFor();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				getLog().info(line);
			}
			
			if (exitValue != 0) {
				throw new MojoExecutionException("Executing "+cmdList.get(0)+" failed with exit value "+exitValue);
			}

			getLog().info("Command: " + cmdList.get(0) + " executed sucsessfully");

			project.addCompileSourceRoot(outputFolder.getAbsolutePath());
			getLog().info("Folder: " + outputFolderPath + " added to the compile source root");

			List<String> excludes = null;
			List<String> includes = Collections.singletonList("**/*.proto");
			projectHelper.addResource(project, inputFolderPath, includes, excludes);
			getLog().info("Added .proto files as resources...");

		} catch (IOException e) {
			throw new MojoExecutionException(
					"An error occurred while executing the protoc command", e);

		} catch (InterruptedException e) {
			throw new MojoExecutionException(
					"An error occurred while executing the protoc command", e);
		} finally {
			IOUtils.closeQuietly(reader);
		}

	}

}
