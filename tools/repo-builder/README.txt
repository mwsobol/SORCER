The directory contains scripts and files needed to build the repository for use with the Engineering application. 
Instructions are as follows:

1. First build modules/sorcer to get all the Sorcer jars available

2. If you have an updated local Maven repository, that includes all the relevant Maven central (and other repository)
artifacts (), you can skip this step. Otherwise, this step is best executed while connected to the internet. If
just doing a Rio update, you can build Rio locally, it's artifacts will be installed during the build process.
To populate the local repository, run: mvn -f poms/repository-deps.pom dependency:tree

3. Change the version of the modified software in the versions.properties file

4. Once complete, run the Ant file (build.xml) This will create a repository in the current directory called repository.
The Ant file contains the following targets:

Main targets:

 all         Builds the entire Repository
 buildtools  Builds the build tools artifacts for the Repository
 common      Builds the Common artifacts for the Repository
 push        Pushes modified repository files to a web server serving the shared repository
 sorcer      Builds the Sorcer artifacts for the Repository

The Ant file is provided as a convenience for using the buildrepo.groovy script. The buildrepo.groovy script installs
the sorcer, buildtools and common jars that are not available from a public Maven repository. The buildrepo.groovy
script also:

- Uses the versions.properties file to obtain the version number for artifacts to install. If the
version number is not the same as the version number in the artifact's pom, the artifact's pom will be updated.

- If the jars being installed have not changed, the script will not update the local repository.

- If the script is called with the "push" option, the script will determine what files have changed, and send the
changed files to the web server that is serving up the repository. Note that if an NFS mounted directory is used, we can
change this to just copy the updated repository contents.

You can call the buildrepo.groovy script directly from the command line, the script takes the following options:

sorcer
buildtools
common
push

For example, to build and update the repository with updated Sorcer artifact run:

./buildrepo.groovy sorcer push

or

ant sorcer push

5. Once the repository has been updated, go to $ENG_HOME/ivy/versions.properties, update the necessary version numbers.
The next time ENG is built, you will get the updates from the repository. NOTE: If version numbers are not changed,
the only way to get the repository updated is to blw away your Ivy cache. This will result in rebuilding you local
repository with the latest from the shared repository.

Other Information
=================
The poms/ directory contain Maven pom files for jars that are not Maven produced. These poms allow the jars to get
installed as Maven artifacts into the local Maven repository.

The .ivy2/cache directory is Ivy's cache. It gets removed and populated each time buildrepo.groovy is run

The tmp/ directory is created if any of the jars in iGrid/lib contain version numbers. Temporary copies of those
jars are created without the version numbers so they get installed correctly into the local Maven repository.

Update local iGrid repo jars
rm -rf /Volumes/SSD.m2/repository/org/sorcer
ant sorcer
cat toPush
ant push

Now to make sure you get the latest from the *new* sorcer, in the
Engineering project make sure the clean-cache target is run before a build