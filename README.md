sbt-proguard
============

[sbt] plugin for running [ProGuard]. This plugin requires sbt 0.12.


Add plugin
----------

Add plugin to `project/plugins.sbt`. For example:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.1.0")
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


Merging Duplicates
------------------

There is no built-in solution for merging duplicated files with sbt-proguard. It
is possible, however, to simply use [sbt-assembly] to do the merging and then
apply proguard to the assembled result.

You can have the output from [sbt-assembly] as the only proguard input with:

```scala
ProguardKeys.filteredInJars in Proguard <<= AssemblyKeys.assembly map ProguardOptions.noFilter
```


Sample projects
---------------

There are [runnable sample projects][samples] included as sbt scripted tests.


Mailing list
------------

Please use the [sbt mailing list][email] and prefix the subject with `[sbt-proguard]`.


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
[samples]: https://github.com/sbt/sbt-proguard/tree/master/src/sbt-test
[email]: http://groups.google.com/group/simple-build-tool
[cla]: http://www.typesafe.com/contribute/cla
[gpl]: http://www.gnu.org/licenses/gpl.html
[except]: http://proguard.sourceforge.net/GPL_exception.html
[apache]: http://www.apache.org/licenses/LICENSE-2.0.html
