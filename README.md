# resources-plugin
# Why?

After some android development I got used to R file approach. And later I found it really useful to have constants automagically generated from the resources folder.
It is MUCH easier to support automatic constants in one place and compile-time issue notification, comparing to having lots of strings EVERYWHERE and run-time notification.

In my case the end result is like:
```
//To load layout file
setLayout(R.layout.menu_team_assembly);

//To work with actors
tblTeamList = (Table) findActor(R.id.tbl_team_list);
registerMenuItem(findButton(R.id.btn_back));

//Actor logic handling
@Override
public void onInteract(Actor actor, InputEvent event) {
    switch (actor.getName()) {
        case R.id.btn_back:
            getSceneDelegate().onBack();
            break;
    }
}

```

## How to use
This gradle plugin is used to process the files in assets folder and generate the Class source with internal file path to use it with AssetsManager
1. Add this line to root project `buildscript`.`dependencies`
```
classpath 'org.arnoid.resources:resources-plugin:0.0.1'
```
2. In target peoject add following line
```
apply plugin: 'org.arnoid.resources.resources-plugin'
```
3. In target project add following configuration
```
resources {
    assetsDir="$projectDir/assets/" //this is mandatory line

    outputDir = "generated-ids/source/ids/main/" //optional

    resourceClass = "R" //optional

    imagesDir = "drawable" //optionals
    fontsDir = "font" //optional
    skinsDir = "skin" //optional
    atlasesDir = "atlas" //optional
    jsonsDir = "json" //optional
    stringsDir = "string" //optional
    layoutsDir = "layout" //optional

    imageResourceClass = "image" //optional
    fontResourceClass = "font" //optional
    skinResourceClass = "skin" //optional
    atlasResourceClass = "atlas" //optional
    jsonResourceClass = "json" //optional
    stringResourceClass = "string" //optional
    layoutResourceClass = "layout" //optional
    idResourceClass = "id" //optional

    idResourceAttributeName = "name" //optional

    imageExtension = "png" //optional
    fontExtension = "fnt" //optional
    skinExtension = "json" //optional
    atlasExtension = "atlas" //optional
    jsonExtension = "json" //optional
    stringExtension = "json" //optional
    layoutExtension = "xml" //optional


    resolution = "{resolution}" //optional
    resolutions = ["xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi"]//optional, order is important
}
```

## How it works
For images/fonts/skins/atlases/jsons it works same. Plugin will list files matching the given extension in given folder and its subfolders.
List of files will be processed into list of file pathes. After that each file path will be processes into pairs - file name and file path with removed `assetsDir` prefix.
Generated source class will contain field labeled as file name with value contained file path with removed `assetsDir` prefix.
Ex. we are looking for the `pcx` images in `/assets/images` folder

We need to provide following configuration:
```
resources {
    assetsDir="$projectDir/assets/" //this is mandatory line

    imagesDir = "images"

    imageResourceClass = "image"

    imageExtension = "png"
}
```
Task will scan folders and will find the `/assets/images/flash/gordon/face.pcx`
End result will look like:
```
public final class R {
    public static final class image {
        public static final face = "images/flash/gordon/face.pcx"
    }
}
```

Files with same names will override each other generated value.

Same behaviour will be applied for fonts, skins, atlasses and jsons.

### Resolutions

It is possible to provide file path part as resolution dependent.

How it works? When code-generation task will process file path for every found file, it will replace any item from `resolutions` array with `resolution` keyword.

Ex.
```
resources {
    assetsDir="$projectDir/assets/" //this is mandatory line

    imagesDir = "images" //optionals

    imageResourceClass = "image" //optional

    imageExtension = "png" //optional

    resolution = "{resolution}"

    resolutions = ["high", "medium", "low"]
}
```

Imagine we have list of files:
```
/assets/images/high/face.pcx
/assets/images/medium/face.pcx
/assets/images/low/face.pcx
```

Task will process all those files into single field.
```
public final class R {
    public static final class image {
        public static final face = "images/{resolution}/face.pcx"
    }
}
```

Later `R.image.face` can be used in the assets loader with replacement of the `{resolution}` with resolution name.
#### Note 1 - Order
Resolution array order is IMPORTANT, because it will apply all the replacements one after another. So if you will specify `["hdpi","xhdpi"]` the end result will be `"images/x{resolution}/face.pcx"`, because it will replace `hdpi` and it will be unable to replace `xhdpi`.
#### Note 2 - Regex
Resolutions array values can be regular expressions

### Strings
Strings are processed slightly in different way. After all the strings files are found (which is done in the way as it is done with previous resources) task will try to process files as **JSON** files with key-value dictionaries, like
```
{
    "title":"Window",
    "buttonName":"button"
}
```

End result will be following:

```
public final class R {
    public static final class string {
        public static final title = "title",
        public static final buttonName = "buttonName"
    }
}
```

### Layouts and Ids
Layouts are processed same ways as other resources, while files list with layouts will be passed to id-processor. Those files will be processed as **XML** files.
Task will scan for tags with `idResourceAttributeName` (default value `name`) attribute and will use its value.
Example file /assets/layouts/scene.xml:
```
<Container
        fillParent="true">
    <Window
            title="@string/menu_audio_window_title">
        <TextButton
                name="btn_back"
                pad="10"
                width="250"
                align="center"
                text="@string/menu_audio_btn_back"/>
    </Window>
</Container>
```

This will be processed into:
```
public final class R {
    public static final class layout {
        public static final scene = "layouts/scene.xml"
    }
    public static final class id {
        public static final btn_back = "btn_back",
    }
}
```
### _all field

All classes will have `_all` `Map<String, String>` field which will contain the all the field values from generated class.
This is done to provide support with resources processing.

Ex. we have a layout xml file
```
<Table
    name="tbl_team_list">
        <Image
            name="btn_back_1"
            pad="10"
            width="128"
            height="128"
            align="center"
            src="@image/helmet_astronaut_logo"/>
</Table>
```
Instead of doing the class field scanning and analysis, we can ask for the image from `_all` field like this `R.image._all("helmet_astronaut_logo")`


# TODO:
Locale support