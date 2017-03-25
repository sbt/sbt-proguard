sbt-proguard
============

[sbt] plugin for running [ProGuard]. This plugin requires sbt 0.12 or 0.13.

[![Build Status](https://travis-ci.org/sbt/sbt-proguard.png?branch=master)](https://travis-ci.org/sbt/sbt-proguard)


Add plugin
----------

Add plugin to `project/plugins.sbt`. For example:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.2")
```


Example
-------

A simple `build.sbt` with settings to configure sbt-proguard:

```scala
proguardSettings

ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

ProguardKeys.options in Proguard += ProguardOptions.keepMain("some.MainClass")
```

Run proguard at the sbt shell with:

```shell
proguard:proguard
```


Filters
-------

Proguard supports file filtering for inputs, libraries, and outputs. In
sbt-proguard there are `File => Option[String]` settings for adding filters to
files.

For example, to add a `!META-INF/**` filter to just the scala-library jar:

```scala
ProguardKeys.inputFilter in Proguard := { file =>
  file.name match {
    case "scala-library.jar" => Some("!META-INF/**")
    case _                   => None
  }
}
```

which will create the following proguard configuration:

```
-injars "/path/to/scala-library.jar"(!META-INF/**)
```

There are corresponding settings for libraries and outputs: `libraryFilter` and
`outputFilter`.

For more advanced usage the `filteredInputs`, `filteredLibraries`, and
`filteredOutputs` settings can be set directly.


Merging
-------

If the same path exists in multiple inputs then proguard will throw an error.
The conflicting paths can be resolved using file filters, as described above,
but this is not always the most useful approach. For example, `reference.conf`
files for the Typesafe Config library need to be retained and not discarded.

The sbt-proguard plugin supports pre-merging inputs, similar to creating an
assembly jar first. To enable this merging use:

```scala
ProguardKeys.merge in Proguard := true
```

Conflicting paths that are not identical will now fail at the merge stage. These
conflicting paths can have merge strategies applied, similar to the [sbt-assembly]
plugin.

Helper methods for creating common merges are available. These are:

  - `discard` -- discard all matching entries
  - `first` -- only keep the first entry
  - `last` -- only keep the last entry
  - `rename` -- rename entries adding the name of the source
  - `append` -- append entries together into one file

The paths matched against in these helpers are normalised to be separated by `/`
regardless of platform. Paths can be matched exactly with a string or with a
regular expression.

The default strategy is to only discard `META-INF/MANIFEST.MF`. This same
strategy could be added with:

```scala
ProguardKeys.mergeStrategies in Proguard += ProguardMerge.discard("META-INF/MANIFEST.MF")
```

Or all `META-INF` contents could be discarded with a regular expression:

```scala
ProguardKeys.mergeStrategies in Proguard += ProguardMerge.discard("META-INF/.*".r)
```

To concatenate all `reference.conf` files together use:

```scala
ProguardKeys.mergeStrategies in Proguard += ProguardMerge.append("reference.conf")
```

To discard all `.html` and `.txt` files you may use two strategies together:

```scala
ProguardKeys.mergeStrategies in Proguard ++= Seq(
  ProguardMerge.discard("\\.html$".r),
  ProguardMerge.discard("\\.txt$".r) 
)
```

Completely custom merge strategies can also be created. See the plugin source
code for how this could be done.


Sample projects
---------------

There are some [runnable sample projects][samples] included as sbt scripted tests.


Contribution policy
-------------------

Contributions via GitHub pull requests are gladly accepted from their original
author. Before we can accept pull requests, you will need to agree to the
[Typesafe Contributor License Agreement][cla] online, using your GitHub account.


License
-------

[ProGuard] is licensed under the [GNU General Public License][gpl]. sbt and sbt scripts
are included in a [special exception][except] to the GPL licensing.

The code for this sbt plugin is licensed under the [Apache 2.0 License][apache].


[sbt]: https://github.com/sbt/sbt
[ProGuard]: http://proguard.sourceforge.net/
[sbt-assembly]: https://github.com/sbt/sbt-assembly
[samples]: https://github.com/sbt/sbt-proguard/tree/v0.2.2/src/sbt-test/proguard
[cla]: http://www.typesafe.com/contribute/cla
[gpl]: http://www.gnu.org/licenses/gpl.html
[except]: http://proguard.sourceforge.net/GPL_exception.html
[apache]: http://www.apache.org/licenses/LICENSE-2.0.html
