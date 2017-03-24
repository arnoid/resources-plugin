package org.arnoid.resources

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import javax.lang.model.element.Modifier
import java.lang.reflect.Type

// TODO Rename this class
class GenerateResourcesTask extends DefaultTask {
    // TODO Edit this value to your desired task name. Note that this value will be used by users in command line.
    public static String NAME = 'generateResources'
    ResourcesExtension extension

    GenerateResourcesTask() {
        project.afterEvaluate {
            project.logger.debug("Registering extension ${ResourcesExtension.NAME}")
            extension = project.extensions."${ResourcesExtension.NAME}"

            String generatedSourceDir = "$project.buildDir/" + extension.outputDir

            project.logger.debug("Registering source dir for generated sources [" + generatedSourceDir + "]")
            project.sourceSets.main.java.srcDirs += generatedSourceDir
        }
    }

    @TaskAction
    void exec() {
        def projectBuildDir = project.buildDir
        def projectDir = project.projectDir

        logger.info("Project build dir [" + projectBuildDir + "]")
        logger.info("Project dir [" + projectDir + "]")

        File generatedSrcDir = new File(projectBuildDir, extension.outputDir)

        logger.info "Generated ids destination [" + generatedSrcDir.absolutePath + "]"

        if (generatedSrcDir.exists()) {
            project.logger.debug("Generated sources dir already exists")
        } else {
            project.logger.debug("Generated sources dir is missing and ")
            generatedSrcDir.mkdirs()
        }

        project.sourceSets.main.java.srcDirs += "$projectBuildDir/" + generatedSrcDir.absolutePath

        String baseAssetsFolder = extension.assetsDir

        TypeSpec.Builder resourcesTypeBuilder = TypeSpec.classBuilder(extension.resourceClass).addModifiers(Modifier.PUBLIC, Modifier.FINAL)

        project.logger.debug("Processing images")
        processResource(extension.imagesDir, extension.imageResourceClass, extension.imageExtension, baseAssetsFolder, resourcesTypeBuilder)
        project.logger.debug("Processing fonts")
        processResource(extension.fontsDir, extension.fontResourceClass, extension.fontExtension, baseAssetsFolder, resourcesTypeBuilder)
        project.logger.debug("Processing skins")
        processResource(extension.skinsDir, extension.skinResourceClass, extension.skinExtension, baseAssetsFolder, resourcesTypeBuilder)
        project.logger.debug("Processing atlases")
        processResource(extension.atlasesDir, extension.atlasResourceClass, extension.atlasExtension, baseAssetsFolder, resourcesTypeBuilder)
        project.logger.debug("Processing jsons")
        processResource(extension.jsonsDir, extension.jsonResourceClass, extension.jsonExtension, baseAssetsFolder, resourcesTypeBuilder)

        project.logger.debug("Processing strings")
        processStringResource(extension.stringsDir, extension.stringResourceClass, extension.stringExtension, baseAssetsFolder, resourcesTypeBuilder)

        project.logger.debug("Processing layouts")
        List<File> layoutFiles = processLayoutResource(baseAssetsFolder, extension.layoutsDir, extension.layoutExtension, resourcesTypeBuilder)

        project.logger.debug("Processing ids")
        processIdResource(layoutFiles, extension.idResourceClass, resourcesTypeBuilder)

        project.logger.debug("Generating main resource class")
        JavaFile.Builder idsJavaFileBuilder = JavaFile.builder(String.valueOf(project.group), resourcesTypeBuilder.build())

        JavaFile javaFile = idsJavaFileBuilder.build()

        project.logger.debug("Writing class resources")
        javaFile.writeTo(generatedSrcDir)
    }

    private void processIdResource(List<File> layoutFiles, String resourceClassName, TypeSpec.Builder resourcesTypeBuilder) {
        Set<String> ids = readIdsFromLayouts(layoutFiles)
        TypeSpec.Builder idsTypeBuilder = produceResourcesTypeBuilder(resourceClassName, ids)
        resourcesTypeBuilder.addType(idsTypeBuilder.build())
    }

    private List<File> processLayoutResource(String baseAssetsFolder, String resourceFolder, String resourceFileExtension, TypeSpec.Builder resourcesTypeBuilder) {
        String layoutsAssetsFolder = baseAssetsFolder + resourceFolder

        List<File> layoutFiles = listFiles(layoutsAssetsFolder, resourceFileExtension)
        Set<String> layouts = readFileNames(layoutFiles)
        TypeSpec.Builder layoutsTypeBuilder = produceResourcesTypeBuilder(resourceFolder, layouts, baseAssetsFolder)
        resourcesTypeBuilder.addType(layoutsTypeBuilder.build())
        return layoutFiles
    }

    private void processResource(String resourceFolderSubFolder, String resourceClassName, String extension, String baseAssetsFolder, TypeSpec.Builder resourcesTypeBuilder) {
        String resourceAssetsFolder = baseAssetsFolder + resourceFolderSubFolder

        List<File> files = listFiles(resourceAssetsFolder, extension)
        Set<String> filePathes = readFileNames(files)
        TypeSpec.Builder resourceTypeBuilder = produceResourcesTypeBuilder(resourceClassName, filePathes, baseAssetsFolder)
        resourcesTypeBuilder.addType(resourceTypeBuilder.build())
    }

    private void processStringResource(String resourceFolderSubFolder, String resourceClassName, String fileExtension, String baseAssetsFolder, TypeSpec.Builder resourcesTypeBuilder) {
        Gson gson = new Gson()
        String stringsAssetsFolder = baseAssetsFolder + resourceFolderSubFolder

        List<File> files = listFiles(stringsAssetsFolder, fileExtension)

        Map<String, String> strings = new HashMap<String, String>()

        for (File file : files) {
            readStringNames(file, gson, strings)
        }

        CodeBlock.Builder initializerBlockBuilder = CodeBlock.builder()

        TypeSpec.Builder stringsTypeBuilder = TypeSpec.classBuilder(resourceClassName).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)

        stringsTypeBuilder.addField(produceMapOfAllResourceValuesFieldSpec(produceMapOfAllResourceValuesType()))

        for (String key : strings.keySet()) {

            stringsTypeBuilder.addField(
                    FieldSpec.builder(String.class, key, Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC).initializer('$S', strings.get(key)).build()
            )
            addMapOfAllResourceValuesInitializerBlockLine(initializerBlockBuilder, key, strings.get(key))
        }

        stringsTypeBuilder.addStaticBlock(initializerBlockBuilder.build())

        resourcesTypeBuilder.addType(stringsTypeBuilder.build())
    }

    private TypeSpec.Builder produceResourcesTypeBuilder(String resourceName, Collection<String> nameSource, String baseAssetsFolder) {
        TypeSpec.Builder dataTypeBuilder = TypeSpec.classBuilder(resourceName).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)

        dataTypeBuilder.addField(produceMapOfAllResourceValuesFieldSpec(produceMapOfAllResourceValuesType()))

        CodeBlock.Builder initializerBlockBuilder = CodeBlock.builder()

        for (String value : nameSource) {

            String internalPath = value.replaceAll(baseAssetsFolder, '')

            String fileName = internalPath.find(~/(\w+[-]?\w+)*(?:\.(\w)+?)*$/)
            String fieldName = fileName.replaceFirst(~/\.[^\.]+$/, '').replaceAll("-", "_")
            dataTypeBuilder.addField(
                    FieldSpec.builder(String.class, fieldName, Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
                            .initializer('$S', internalPath)
                            .build()
            )

            addMapOfAllResourceValuesInitializerBlockLine(initializerBlockBuilder, fieldName, internalPath)
        }

        dataTypeBuilder.addStaticBlock(initializerBlockBuilder.build())

        return dataTypeBuilder
    }

    private CodeBlock.Builder addMapOfAllResourceValuesInitializerBlockLine(CodeBlock.Builder initializerBlockBuilder, String key, String value) {
        initializerBlockBuilder.add('$L.put($S,$S);\n', extension.mapOfAllReourceValuesFieldName, key, value)
    }

    private FieldSpec produceMapOfAllResourceValuesFieldSpec(ParameterizedTypeName mapOfAllResources) {
        FieldSpec.builder(mapOfAllResources, extension.mapOfAllReourceValuesFieldName, Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
                .initializer('new $T()', mapOfAllResources)
                .build()
    }

    private TypeSpec.Builder produceResourcesTypeBuilder(String resourceName, Collection<String> nameSource) {
        TypeSpec.Builder dataTypeBuilder = TypeSpec.classBuilder(resourceName).addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)

        dataTypeBuilder.addField(produceMapOfAllResourceValuesFieldSpec(produceMapOfAllResourceValuesType()))

        CodeBlock.Builder initializerBlockBuilder = CodeBlock.builder()

        for (String value : nameSource) {

            def key = value.replaceFirst(~/\.[^\.]+$/, '')
            dataTypeBuilder.addField(
                    FieldSpec.builder(String.class, key, Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
                            .initializer('$S', value)
                            .build()
            )

            addMapOfAllResourceValuesInitializerBlockLine(initializerBlockBuilder, key, value);
        }

        dataTypeBuilder.addStaticBlock(initializerBlockBuilder.build())

        return dataTypeBuilder
    }

    private Set<String> readIdsFromLayouts(List<File> layoutFiles) {
        Set<String> ids = new ArrayList<>()

        logger.info("Extracting ids from layoutsDir")
        for (File layoutFile : layoutFiles) {
            logger.info("Processing layout [" + layoutFile.getAbsolutePath() + "]")
            Node items = new XmlParser().parse(layoutFile)
            fetchNodeId(items, ids)
            fetchChildrenIds(items, ids)
        }

        return ids
    }

    private void fetchChildrenIds(Node item, final Set<String> ids) {
        List children = item.children()

        for (Node child : children) {
            logger.debug("Found item [" + item + "]")
            fetchNodeId(child, ids)
            fetchChildrenIds(child, ids)
        }
    }

    protected void fetchNodeId(Node item, Set<String> ids) {
        if (item.hasProperty(extension.idResourceAttributeName)) {
            String name = item.attributes().get(extension.idResourceAttributeName)
            if (name != null && name.length() > 0) {
                logger.info("Found id [" + name + "]")
                ids.add(name)
            }
        }
    }

    protected Map<String, String> readStringNames(File file, Gson gson, Map<String, String> stringsMap) {
        logger.info "Parsing file [" + file.absolutePath + "]"

        project.logger.debug("Reading string names from file [" + file.path + "]")

        Reader reader = new FileReader(file)

        Type type = new TypeToken<Map<String, String>>() {
        }.getType()

        Map<String, String> strings = gson.fromJson(reader, type)

        stringsMap.putAll(strings)

        reader.close()

        return stringsMap
    }

    private Set<String> readFileNames(List<File> files) {
        project.logger.debug("Reading file names")
        Set<String> strings = new HashSet<>(files.size())

        files.each { File file ->
            String path = file.path
            for (String resolutionName : extension.resolutions) {
                path = path.replaceAll(resolutionName, extension.resolution)
            }

            strings.add(path)
        }

        return strings
    }

    protected List<File> listFiles(String baseFolder, String extension) {
        project.logger.debug("Listing files with extension [" + extension + "]in folder [" + baseFolder + "]")
        List<File> files = new ArrayList<>()

        File filesFolder = new File(baseFolder)

        if (filesFolder.exists()) {
            filesFolder.listFiles().each { File file ->
                if (file.directory) {
                    files.addAll(listFiles(file.getAbsolutePath(), extension))
                } else if (file.name.endsWith(extension)) {
                    project.logger.debug("Found file [" + file.getAbsolutePath() + "]")
                    files.add(file)
                }
            }
        }

        return files
    }

    private static ParameterizedTypeName produceMapOfAllResourceValuesType() {
        ClassName keyClassName = ClassName.get("java.lang", "String")
        ClassName valueClassName = ClassName.get("java.lang", "String")
        ClassName mapClassName = ClassName.get("java.util", "HashMap")
        TypeName mapOfAllResources = ParameterizedTypeName.get(mapClassName, keyClassName, valueClassName)
        mapOfAllResources
    }

}
