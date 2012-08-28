package com.assembla.jenkins

class Check {
    public static void main(String[] args) {
        def api = new Api(assemblaServerUrl: 'https://www.core.m-stage.assembla.cc/',
                user: 'super', password: 'super',
                // https://core.m-stage.assembla.cc/user/edit/manage_clients
                clientId: 'cVbBqc8r8r4BH5eJe7dGu1', clientToken: 'b825a30264abc8da03d5d88556147592',
                spaceId: 'groovy', spaceToolId: 'git')
        api.mergeRequestBranchNames()
    }
}
