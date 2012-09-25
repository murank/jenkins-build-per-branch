package com.assembla.jenkins

class Check {
    static String apiUrl = 'https://core.m-stage.assembla.cc/'
    static String clientId = 'bovv_mbMqr4O8HeJe9alzu'
    static String clientToken = '59d576d8ae2cd19ad0c3512323b08ec2'

    public static void main(String[] args) {
        //checkMrAPI()
        setup()

    }

    private static void checkMrAPI() {

        clientId = 'cVbBqc8r8r4BH5eJe7dGu1'
        clientToken = 'b825a30264abc8da03d5d88556147592'
        def api = new Api(assemblaServerUrl: apiUrl,
                user: 'super', password: 'super',
                // https://core.m-stage.assembla.cc/user/edit/manage_clients
                clientId: clientId, clientToken: clientToken,
                spaceId: 'groovy', spaceToolId: 'git')
        println api.mergeRequestBranchNames
    }

    public static void setup() {
        def api = new Api(assemblaServerUrl: apiUrl, clientId: clientId, clientToken: clientToken)

        SetupApi.setupApi(api)
    }
}
