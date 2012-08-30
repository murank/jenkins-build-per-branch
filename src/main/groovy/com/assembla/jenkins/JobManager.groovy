package com.assembla.jenkins

import com.entagen.jenkins.JenkinsJobManager
import com.entagen.jenkins.TemplateJob

class JobManager extends JenkinsJobManager {

    String assemblaUrl
    String assemblaUser
    String assemblaPassword
    String clientId
    String clientToken
    String spaceId
    String spaceToolId

    private Api assemblaApi

    JobManager(Map props) {
        super(props)
        initApi()
    }

    void syncWithMergeRequests() {
        List<String> allBranchNames = assemblaApi.mergeRequestBranchNames
        List<String> allJobNames = jenkinsApi.jobNames

        // ensure that there is at least one job matching the template pattern, collect the set of template jobs
        List<TemplateJob> templateJobs = findRequiredTemplateJobs(allJobNames)

        // create any missing template jobs and delete any jobs matching the template patterns that no longer have branches
        syncJobs(allBranchNames, allJobNames, templateJobs)

        // create any missing branch views, scoped within a nested view if we were given one
        if (!noViews) {
            syncViews(allBranchNames)
        }
    }

    Api initApi() {
        if (!assemblaApi) {
            assert clientId != null
            assert clientToken != null
            assert assemblaUser != null
            assert assemblaPassword != null
            this.assemblaApi = new Api(assemblaServerUrl: assemblaUrl,
                    clientId: clientId, clientToken: clientToken,
                    spaceId: spaceId, spaceToolId: spaceToolId,
                    user: assemblaUser, password: assemblaPassword
            )
            // this.gitUrl = assemblaApi.spaceTool.gitUrl TODO wait for space tool API
        }

        return this.assemblaApi
    }
}
