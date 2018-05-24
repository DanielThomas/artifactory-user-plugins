Artifactory Remove Module Dependencies Plugin
================================================

This plugin will remove all of the dependencies from inside the `buildInfo.json` of all the build's modules.

#### Installation
To install this plugin:
  - Place the script under the master Artifactory instance in the
  `${ARTIFACTORY_HOME}/etc/plugins`
  - Verify in the `${ARTIFACTORY_HOME}/logs/artifactory.log`, after starting up
  the instance, that the plugin was loaded correctly
