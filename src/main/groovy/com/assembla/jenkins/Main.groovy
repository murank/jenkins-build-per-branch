package com.assembla.jenkins

import com.entagen.jenkins.CliTools

/*
Bootstrap class that parses command line arguments, or system properties passed in by jenkins, and starts the jenkins-build-per-branch sync process
*/
class Main extends CliTools {

    public static void main(String[] args) {
        Map<String, String> argsMap = parseArgs(args)
        showConfiguration(argsMap)
        JobManager manager = new JobManager(argsMap)
        manager.syncWithMergeRequests()
    }
}
