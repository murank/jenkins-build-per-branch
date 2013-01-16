# Jenkins Assembla Merge Request builder (JAMRb) #

Auto-create Jenkins build jobs for new Assembla GIT merge requests

## About ##

The code in this repo lets you automatically generate Jenkins jobs for new merge requests in a specified repository, using templates to create the jobs.

Using it is as easy as:

* Creating the set of jobs for your master branch that will be used as templates for feature branches, test them so that they work with master
* Make a new job that is in charge of creating new builds for each merge request in jenkins using JAMRb code

This job will be in charge of both creating new jobs for new merge requests as well as cleaning up old merge request jobs that have been fully merged into another branch

## Installation ##

It has the following plugin requirements in Jenkins:

* Jenkins Git Plugin
* Jenkins Gradle plugin
* the git command line app also must be installed (this is already a requirement of the Jenkins Git Plugin), and it must be configured with credentials (likely an SSH key) that has authorization rights to the git repository
* Additional optional functionality for grouping your feature-branch builds is enabled if you have the Nested View Plugin installed, but it is not required.
* There are a few ways to use JAMRb, but the easiest is probably to make a new Jenkins job that runs on a cron timer (every 5 minutes or so).

You'll want to already have your template jobs running and passing (likely one or more jobs on the master branch of your repo).

Then, create a new Jenkins job called SyncYOURPROJECTMergeRequests as a "free-style software project".

Set the job to clone the JAMRb repo from Assembla with the repository url git://git.assembla.com/assembla-oss.JAMRb.git

The branch to build should be set to origin/master.

Check the box to have it "build periodically" and set the cron expression to */5 * * * * to get it building every 5 minutes.

Click on the "Add build step" dropdown and pick "Invoke Gradle script" from the dropdown.

In the "Switches" box, enter the system parameters unique to your jenkins setup and git install, here's an example that you can use to tweak:

	-DjenkinsUrl=http://localhost:8080/
	-DgitUrl=git://git.assembla.com:space.git
	-DtemplateJobPrefix=MyProject
	-DtemplateBranchName=master
	-DnestedView=MyProject-branches
	-DapiKey=api-key-to-access-assembla
	-DapiSecret=api-secret-to-access-assembla
	-DspaceId=your-space-url-name
	-DspaceToolId=git
	-DdryRun=true

Those switches include a -DdryRun=true system parameter so that the initial run does not change anything, it only logs out what it would do.

In the "Tasks" field, enter syncWithMergeRequests.

Save the job and then "Build Now" to see if you've got things configured correctly. Look at the output log to see what's happening. If everything runs without exceptions, and it looks like it's creating the jobs and views you want, remove the -DdryRun=true flag and let it run for real.

This job is potentially destructive as it will delete old feature branch jobs for feature branches that no longer exist. It's strongly recommended that you back up your jenkins jobs directory before running, just in case.

## Script System Parameter Details ##

The following options are available in the script:

* **-DjenkinsUrl** - ex: http://localhost:8080/ **(mandatory)** - the url to jenkins, you should be able to append api/json to the url to get a json feed.
* **-DjenkinsUser** - ex: admin (optional) - the username for Jenkins HTTP basic auth.
* **-DjenkinsPassword** - ex: sekr1t (optional) - the password (or API token, obtainable in http://localhost:8080/user/admin/configure if you are using [Jenkins Assembla Auth](https://www.assembla.com/code/assembla-oss/git/nodes)) for Jenkins HTTP basic auth.
* **-DgitUrl** - ex: git://git.assembla.com:space.git **(mandatory)** - the url to the git repo, read-only git url preferred.
* **-DtemplateJobPrefix** - ex: myproj **(mandatory)** - the prefix that the template jobs (and all newly created jobs) will have, likely the project name, the view containing all of the branch's jobs will also use this prefix.
* **-DtemplateBranchName** - ex: master **(mandatory)** - the branch name with jobs in jenkins that's used as a template for all feature branches, this will be the suffix replaced for each branch.
* **-DnestedView** - ex: MyProject-feature-branches (optional) - the name of the existing nested view that will hold a view per feature branch, reqires the Nested View plugin to be installed. This is useful to avoid an explosion of tabs in Jenkin's UI. Without this parameter, each branch will get it's jobs in it's own tab at the top.
* **-DapiKey** - ex: api-key-to-access-assembla **(mandatory)** - API key to access Assembla. Find yours [here](https://www.assembla.com/user/edit/manage_clients).
* **-DapiSecret** - ex: api-secret-to-access-assembla **(mandatory)** - API key secret to aceess Assembla. Find yours [here](https://www.assembla.com/user/edit/manage_clients).
* **-DspaceId** - your-space-url-name **(mandatory)** - the name of the space from the url.
* **-DspaceToolId** - ex: git **(mandatory)** - the space tool url name. For first git tool it is git, for second git-2 etc.
* **-DskipUpstream** - ex: true (optional) - if this is set, it will not build merge requests to upstream. This is useful if you work in forks and submit merge requests from fork/master to upstream/master. Usually, your template build will be already based on master, so you don't need the sync job to create jobs for this.
* **-DdryRun** - ex: true - if this flag is passed with any value, it will not execute any changes only print out what would happen.
* **-DstartOnCreate** - ex: build (optional) - this can be set to build or buildWithParameters to trigger the build with appropriote method after a new job is created

## Conventions ##

It is expected that there will be 1 or more "template" jobs that will be replicated for each new branch that is created. In most workflows, this will likely be the jobs for the master branch.

The job names are expected to be of the format:

	<templateJobPrefix>-<jobName>-<templateBranchName>

* templateJobPrefix is probably the name of the git repository, ex: MyProject
* jobName describes what Jenkins is actually doing with the job, ex: runTests
* templateBranchName is the branch that's being used as a template for the feature branch jobs, ex: master

So you could have 3 jobs that are dependent on each other to run, ex:

	MyProject-RunTests-master
	MyProject-BuildWar-master
	MyProject-DeployApp-master

If you created a new merge request (1) from branch ticket-9 to master, it would create these jobs in Jenkins:

	MyProject-RunTests-1-ticket-9-master
	MyProject-BuildWar-1-ticket-9-master
	MyProject-DeployApp-1-ticket-9-master

It will also create a new view for the branch to contain all 3 of those jobs called "MyProject-1-ticket-9-master". If you haven't used the nestedView parameter, it will be a new tab at the top of the main screen, otherwise it will be contained within that view.

Once ticket-9 was tested, accepted, and merged into master, the sync job would then delete those jobs and it's view on the next run.

## Building a Merge Request ##

Sometimes you might want to build not the underlying source branch of a merge request, but the actual merge result - source merged into target. Make your template job that is building origin/master a parameterized build.

Use String parameter MERGE_REMOTE to specify the remote repo (Default value ex: git://git.assembla.com:your-space.git) and MERGE_BRANCH to specify the remote branch (Default value ex: master). For each job added by JAMRb, default values for these parameters will be changed to target remote and target branch from the merge request. Also, add a build step to do the actual merge - Execute Shell - `git pull $MERGE_REMOTE $MERGE_BRANCH`.

## Thanks ##

Thanks to [Ted Naleid](https://github.com/tednaleid) for creating the original [jenkins-build-per-branch](https://github.com/entagen/jenkins-build-per-branch), upon which JAMRb is based.

