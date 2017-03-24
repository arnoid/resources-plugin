package org.arnoid.resources

// TODO Rename this class. If you don't need any extensions, you can remove this class.
class ResourcesExtension {
    // TODO Edit this value to appropriate extension name. Note that this value will be seen by users in build.gradle.
    public static final NAME = 'resources'

    String assetsDir//="$projectDir/assets/"

    String outputDir = "generated-ids/source/ids/main/"

    String resourceClass = "R"

    String imagesDir = "drawable"
    String fontsDir = "font"
    String skinsDir = "skin"
    String atlasesDir = "atlas"
    String jsonsDir = "json"
    String stringsDir = "strings"
    String layoutsDir = "layout"

    String imageResourceClass = "image"
    String fontResourceClass = "font"
    String skinResourceClass = "skin"
    String atlasResourceClass = "atlas"
    String jsonResourceClass = "json"
    String stringResourceClass = "string"
    String layoutResourceClass = "layout"
    String idResourceClass = "id"

    String idResourceAttributeName = "name"

    String imageExtension = "png"
    String fontExtension = "fnt"
    String skinExtension = "json"
    String atlasExtension = "atlas"
    String jsonExtension = "json"
    String stringExtension = "json"
    String layoutExtension = "xml"

    String mapOfAllReourceValuesFieldName = "_all"

    String resolution = "{resolution}"
    String[] resolutions = ["xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi"]//order of replacement is important

}
