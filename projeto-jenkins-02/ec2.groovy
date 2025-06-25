// Jenkinsfile para criação de instâncias EC2 com parâmetros dinâmicos reativos
// Author: DevOps Team
// Date: $(date +%Y-%m-%d)

pipeline {

    properties([
        pipelineTriggers([
            // Trigger para GitHub push events
            [$class: "GitHubPushTrigger"],
            // Polling SCM (opcional, mas útil como fallback)
            pollSCM('H/1 * * * *') // Verifica a cada 5 minutos
        ]),
        disableConcurrentBuilds() // Evita builds concorrentes
    ])
    
    agent any
    
    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'prod'],
            description: 'Ambiente de destino (determina as opções disponíveis)'
        )
        
        // Parâmetros reativos usando Active Choices
        activeChoiceReactiveParam(
            name: 'AWS_ACCOUNT',
            description: 'Conta AWS de destino',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        def accounts = [
                            'dev': ['123456789012'],
                            'staging': ['234567890123'],
                            'prod': ['345678901234']
                        ]
                        return accounts[ENVIRONMENT] ?: []
                    ''',
                    fallbackScript: "return ['Erro: Ambiente inválido']"
                ]
            ],
            referencedParameters: ['ENVIRONMENT']
        )
        
        activeChoiceReactiveParam(
            name: 'AWS_REGION',
            description: 'Região AWS',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        def regions = [
                            'dev': ['us-east-1', 'us-west-2'],
                            'staging': ['us-east-1', 'us-west-2', 'eu-west-1'],
                            'prod': ['us-east-1', 'us-west-2', 'eu-west-1', 'ap-southeast-1']
                        ]
                        return regions[ENVIRONMENT] ?: []
                    ''',
                    fallbackScript: "return ['Erro: Ambiente inválido']"
                ]
            ],
            referencedParameters: ['ENVIRONMENT']
        )
        
        string(
            name: 'INSTANCE_NAME',
            defaultValue: 'ec2-instance',
            description: 'Nome da instância EC2 (será prefixado com o ambiente)'
        )
        
        activeChoiceReactiveParam(
            name: 'INSTANCE_TYPE',
            description: 'Tipo da instância EC2',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        def instanceTypes = [
                            'dev': [
                                't3.micro',
                                't3.small',
                                't3.medium'
                            ],
                            'staging': [
                                't3.medium',
                                't3.large',
                                'm5.large',
                                'm5.xlarge'
                            ],
                            'prod': [
                                'm5.large',
                                'm5.xlarge',
                                'm5.2xlarge',
                                'c5.large',
                                'c5.xlarge',
                                'c5.2xlarge'
                            ]
                        ]
                        return instanceTypes[ENVIRONMENT] ?: []
                    ''',
                    fallbackScript: "return ['Erro: Ambiente inválido']"
                ]
            ],
            referencedParameters: ['ENVIRONMENT']
        )
        
        activeChoiceReactiveParam(
            name: 'VPC_ID',
            description: 'VPC onde criar a instância',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        def vpcs = [
                            'dev': [
                                'vpc-dev001 (VPC Principal Dev)',
                                'vpc-dev002 (VPC Secundária Dev)'
                            ],
                            'staging': [
                                'vpc-stg001 (VPC Principal Staging)',
                                'vpc-stg002 (VPC Secundária Staging)'
                            ],
                            'prod': [
                                'vpc-prd001 (VPC Principal Prod)',
                                'vpc-prd002 (VPC Secundária Prod)',
                                'vpc-prd003 (VPC DR Prod)'
                            ]
                        ]
                        return vpcs[ENVIRONMENT] ?: []
                    ''',
                    fallbackScript: "return ['Erro: Ambiente inválido']"
                ]
            ],
            referencedParameters: ['ENVIRONMENT']
        )
        
        activeChoiceReactiveParam(
            name: 'SUBNET_ID',
            description: 'Subnet onde criar a instância',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        def subnets = [
                            'dev': [
                                'subnet-dev001 (Public Subnet AZ-1a)',
                                'subnet-dev002 (Private Subnet AZ-1a)',
                                'subnet-dev003 (Public Subnet AZ-1b)',
                                'subnet-dev004 (Private Subnet AZ-1b)'
                            ],
                            'staging': [
                                'subnet-stg001 (Public Subnet AZ-1a)',
                                'subnet-stg002 (Private Subnet AZ-1a)',
                                'subnet-stg003 (Public Subnet AZ-1b)',
                                'subnet-stg004 (Private Subnet AZ-1b)',
                                'subnet-stg005 (Database Subnet AZ-1a)',
                                'subnet-stg006 (Database Subnet AZ-1b)'
                            ],
                            'prod': [
                                'subnet-prd001 (Public Subnet AZ-1a)',
                                'subnet-prd002 (Private Subnet AZ-1a)',
                                'subnet-prd003 (Public Subnet AZ-1b)',
                                'subnet-prd004 (Private Subnet AZ-1b)',
                                'subnet-prd005 (Database Subnet AZ-1a)',
                                'subnet-prd006 (Database Subnet AZ-1b)',
                                'subnet-prd007 (Public Subnet AZ-1c)',
                                'subnet-prd008 (Private Subnet AZ-1c)',
                                'subnet-prd009 (Database Subnet AZ-1c)'
                            ]
                        ]
                        return subnets[ENVIRONMENT] ?: []
                    ''',
                    fallbackScript: "return ['Erro: Ambiente inválido']"
                ]
            ],
            referencedParameters: ['ENVIRONMENT']
        )
        
        activeChoiceReactiveParam(
            name: 'SECURITY_GROUP_ID',
            description: 'Security Group para a instância',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        def securityGroups = [
                            'dev': [
                                'sg-dev001 (Web Servers Dev)',
                                'sg-dev002 (Database Dev)',
                                'sg-dev003 (Application Servers Dev)',
                                'sg-dev004 (Load Balancers Dev)'
                            ],
                            'staging': [
                                'sg-stg001 (Web Servers Staging)',
                                'sg-stg002 (Database Staging)',
                                'sg-stg003 (Application Servers Staging)',
                                'sg-stg004 (Load Balancers Staging)',
                                'sg-stg005 (Monitoring Staging)'
                            ],
                            'prod': [
                                'sg-prd001 (Web Servers Prod)',
                                'sg-prd002 (Database Prod)',
                                'sg-prd003 (Application Servers Prod)',
                                'sg-prd004 (Load Balancers Prod)',
                                'sg-prd005 (Monitoring Prod)',
                                'sg-prd006 (Bastion Hosts Prod)',
                                'sg-prd007 (NAT Instances Prod)'
                            ]
                        ]
                        return securityGroups[ENVIRONMENT] ?: []
                    ''',
                    fallbackScript: "return ['Erro: Ambiente inválido']"
                ]
            ],
            referencedParameters: ['ENVIRONMENT']
        )
        
        activeChoiceReactiveParam(
            name: 'AMI_ID',
            description: 'AMI a ser utilizada',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        def amis = [
                            'dev': [
                                'ami-dev001 (Ubuntu 22.04 LTS Dev)',
                                'ami-dev002 (Amazon Linux 2023 Dev)',
                                'ami-dev003 (CentOS 9 Dev)',
                                'ami-dev004 (Custom App Image Dev)'
                            ],
                            'staging': [
                                'ami-stg001 (Ubuntu 22.04 LTS Staging)',
                                'ami-stg002 (Amazon Linux 2023 Staging)',
                                'ami-stg003 (CentOS 9 Staging)',
                                'ami-stg004 (Custom App Image Staging)',
                                'ami-stg005 (Hardened Ubuntu Staging)'
                            ],
                            'prod': [
                                'ami-prd001 (Ubuntu 22.04 LTS Prod)',
                                'ami-prd002 (Amazon Linux 2023 Prod)',
                                'ami-prd003 (CentOS 9 Prod)',
                                'ami-prd004 (Custom App Image Prod)',
                                'ami-prd005 (Hardened Ubuntu Prod)',
                                'ami-prd006 (Compliance Ubuntu Prod)',
                                'ami-prd007 (Golden Image Prod)'
                            ]
                        ]
                        return amis[ENVIRONMENT] ?: []
                    ''',
                    fallbackScript: "return ['Erro: Ambiente inválido']"
                ]
            ],
            referencedParameters: ['ENVIRONMENT']
        )
        
        activeChoiceReactiveParam(
            name: 'KEY_PAIR',
            description: 'Chave SSH para acesso',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        def keyPairs = [
                            'dev': [
                                'keypair-dev-general',
                                'keypair-dev-admin',
                                'keypair-dev-automation'
                            ],
                            'staging': [
                                'keypair-stg-general',
                                'keypair-stg-admin',
                                'keypair-stg-automation',
                                'keypair-stg-testing'
                            ],
                            'prod': [
                                'keypair-prd-general',
                                'keypair-prd-admin',
                                'keypair-prd-automation',
                                'keypair-prd-emergency'
                            ]
                        ]
                        return keyPairs[ENVIRONMENT] ?: []
                    ''',
                    fallbackScript: "return ['Erro: Ambiente inválido']"
                ]
            ],
            referencedParameters: ['ENVIRONMENT']
        )
        
        choice(
            name: 'VOLUME_TYPE',
            choices: ['gp3', 'gp2', 'io1', 'io2'],
            description: 'Tipo do volume EBS'
        )
        string(
            name: 'VOLUME_SIZE',
            defaultValue: '20',
            description: 'Tamanho do volume em GB'
        )
        booleanParam(
            name: 'ENABLE_MONITORING',
            defaultValue: false,
            description: 'Habilitar monitoramento detalhado'
        )
        booleanParam(
            name: 'ENABLE_TERMINATION_PROTECTION',
            defaultValue: false,
            description: 'Habilitar proteção contra terminação'
        )
        text(
            name: 'USER_DATA',
            defaultValue: '',
            description: 'Script de inicialização (opcional)'
        )
    }
    
    environment {
        AWS_DEFAULT_REGION = "${params.AWS_REGION}"
        INSTANCE_FULL_NAME = "${params.ENVIRONMENT}-${params.INSTANCE_NAME}"
    }
    
    stages {
        stage('Validar Parâmetros') {
            steps {
                script {
                    validateParameters()
                }
            }
        }
        
        stage('Configurar Ambiente AWS') {
            steps {
                script {
                    configureAwsEnvironment()
                }
            }
        }
        
        stage('Verificar Recursos Existentes') {
            steps {
                script {
                    checkExistingResources()
                }
            }
        }
        
        stage('Criar Instância EC2') {
            steps {
                script {
                    createEc2Instance()
                }
            }
        }
        
        stage('Verificar Status da Instância') {
            steps {
                script {
                    waitForInstanceReady()
                }
            }
        }
        
        stage('Aplicar Tags') {
            steps {
                script {
                    applyTags()
                }
            }
        }
        
        stage('Exibir Informações da Instância') {
            steps {
                script {
                    displayInstanceInfo()
                }
            }
        }
    }
    
    post {
        success {
            echo "✅ Instância EC2 '${env.INSTANCE_FULL_NAME}' criada com sucesso!"
            script {
                sendSuccessNotification()
            }
        }
        failure {
            echo "❌ Falha na criação da instância EC2 '${env.INSTANCE_FULL_NAME}'"
            script {
                sendFailureNotification()
            }
        }
        cleanup {
            cleanWs()
        }
    }
}

// ==========================
// FUNÇÕES DE IMPLEMENTAÇÃO
// ==========================

def validateParameters() {
    echo "🔍 Validando parâmetros de entrada..."
    
    // Validar nome da instância
    if (!params.INSTANCE_NAME || params.INSTANCE_NAME.trim().isEmpty()) {
        error("Nome da instância é obrigatório!")
    }
    
    // Validar tamanho do volume
    def volumeSize = params.VOLUME_SIZE as Integer
    if (volumeSize < 8 || volumeSize > 1000) {
        error("Tamanho do volume deve estar entre 8 e 1000 GB!")
    }
    
    // Validações específicas por ambiente
    if (params.ENVIRONMENT == 'prod') {
        if (!params.ENABLE_TERMINATION_PROTECTION) {
            echo "⚠️  AVISO: Proteção contra terminação não está habilitada para produção!"
        }
        if (volumeSize < 20) {
            error("Volume mínimo para produção é 20 GB!")
        }
    }
    
    echo "✅ Validação de parâmetros concluída com sucesso!"
}

def configureAwsEnvironment() {
    echo "🔧 Configurando ambiente AWS..."
    
    // Configurar credenciais baseadas na conta selecionada
    def accountId = params.AWS_ACCOUNT
    def credentialsId = getCredentialsId(accountId)
    
    withCredentials([aws(credentialsId: credentialsId)]) {
        sh """
            echo "Configurando AWS CLI..."
            aws configure set region ${params.AWS_REGION}
            aws configure set output json
            
            echo "Verificando credenciais..."
            aws sts get-caller-identity
        """
    }
    
    echo "✅ Ambiente AWS configurado com sucesso!"
}

def getCredentialsId(accountId) {
    def credentialsMap = [
        '123456789012': 'aws-dev-credentials',
        '234567890123': 'aws-staging-credentials',
        '345678901234': 'aws-prod-credentials'
    ]
    
    return credentialsMap[accountId] ?: 'aws-dev-credentials'
}

def checkExistingResources() {
    echo "🔍 Verificando recursos existentes..."
    
    def accountId = params.AWS_ACCOUNT
    def credentialsId = getCredentialsId(accountId)
    
    withCredentials([aws(credentialsId: credentialsId)]) {
        // Verificar se já existe uma instância com o mesmo nome
        def existingInstance = sh(
            script: """
                aws ec2 describe-instances \
                    --filters "Name=tag:Name,Values=${env.INSTANCE_FULL_NAME}" \
                              "Name=instance-state-name,Values=running,pending,stopping,stopped" \
                    --query 'Reservations[*].Instances[*].InstanceId' \
                    --output text
            """,
            returnStdout: true
        ).trim()
        
        if (existingInstance) {
            echo "⚠️  Instância com nome '${env.INSTANCE_FULL_NAME}' já existe: ${existingInstance}"
            
            if (params.ENVIRONMENT == 'prod') {
                error("Não é permitido sobrescrever instâncias em produção!")
            }
            
            input message: "Instância já existe. Deseja continuar mesmo assim?", ok: "Sim, continuar"
        }
        
        // Verificar recursos dependentes
        verifyVpcExists()
        verifySubnetExists()
        verifySecurityGroupExists()
        verifyKeyPairExists()
        verifyAmiExists()
    }
    
    echo "✅ Verificação de recursos concluída!"
}

def verifyVpcExists() {
    def vpcId = extractId(params.VPC_ID)
    sh """
        echo "Verificando VPC ${vpcId}..."
        aws ec2 describe-vpcs --vpc-ids ${vpcId} --query 'Vpcs[0].VpcId' --output text
    """
}

def verifySubnetExists() {
    def subnetId = extractId(params.SUBNET_ID)
    sh """
        echo "Verificando Subnet ${subnetId}..."
        aws ec2 describe-subnets --subnet-ids ${subnetId} --query 'Subnets[0].SubnetId' --output text
    """
}

def verifySecurityGroupExists() {
    def sgId = extractId(params.SECURITY_GROUP_ID)
    sh """
        echo "Verificando Security Group ${sgId}..."
        aws ec2 describe-security-groups --group-ids ${sgId} --query 'SecurityGroups[0].GroupId' --output text
    """
}

def verifyKeyPairExists() {
    sh """
        echo "Verificando Key Pair ${params.KEY_PAIR}..."
        aws ec2 describe-key-pairs --key-names ${params.KEY_PAIR} --query 'KeyPairs[0].KeyName' --output text
    """
}

def verifyAmiExists() {
    def amiId = extractId(params.AMI_ID)
    sh """
        echo "Verificando AMI ${amiId}..."
        aws ec2 describe-images --image-ids ${amiId} --query 'Images[0].ImageId' --output text
    """
}

def extractId(fullString) {
    // Extrai o ID do formato "id-123 (Descrição)"
    return fullString.split(' ')[0]
}

def createEc2Instance() {
    echo "🚀 Criando instância EC2..."
    
    def accountId = params.AWS_ACCOUNT
    def credentialsId = getCredentialsId(accountId)
    
    withCredentials([aws(credentialsId: credentialsId)]) {
        def userData = params.USER_DATA ? "--user-data '${params.USER_DATA}'" : ""
        def monitoring = params.ENABLE_MONITORING ? "--monitoring State=enabled" : "--monitoring State=disabled"
        
        def runResult = sh(
            script: """
                aws ec2 run-instances \
                    --image-id ${extractId(params.AMI_ID)} \
                    --count 1 \
                    --instance-type ${params.INSTANCE_TYPE} \
                    --key-name ${params.KEY_PAIR} \
                    --subnet-id ${extractId(params.SUBNET_ID)} \
                    --security-group-ids ${extractId(params.SECURITY_GROUP_ID)} \
                    --block-device-mappings '[{
                        "DeviceName": "/dev/sda1",
                        "Ebs": {
                            "VolumeSize": ${params.VOLUME_SIZE},
                            "VolumeType": "${params.VOLUME_TYPE}",
                            "DeleteOnTermination": true
                        }
                    }]' \
                    ${monitoring} \
                    ${userData} \
                    --tag-specifications 'ResourceType=instance,Tags=[
                        {Key=Name,Value=${env.INSTANCE_FULL_NAME}},
                        {Key=Environment,Value=${params.ENVIRONMENT}},
                        {Key=CreatedBy,Value=Jenkins},
                        {Key=BuildNumber,Value=${env.BUILD_NUMBER}}
                    ]' \
                    --query 'Instances[0].InstanceId' \
                    --output text
            """,
            returnStdout: true
        ).trim()
        
        env.INSTANCE_ID = runResult
        echo "✅ Instância criada com ID: ${env.INSTANCE_ID}"
        
        // Aplicar proteção contra terminação se habilitada
        if (params.ENABLE_TERMINATION_PROTECTION) {
            sh """
                aws ec2 modify-instance-attribute \
                    --instance-id ${env.INSTANCE_ID} \
                    --disable-api-termination
            """
            echo "✅ Proteção contra terminação aplicada"
        }
    }
}

def waitForInstanceReady() {
    echo "⏳ Aguardando instância ficar disponível..."
    
    def accountId = params.AWS_ACCOUNT
    def credentialsId = getCredentialsId(accountId)
    
    withCredentials([aws(credentialsId: credentialsId)]) {
        timeout(time: 10, unit: 'MINUTES') {
            sh """
                aws ec2 wait instance-running --instance-ids ${env.INSTANCE_ID}
                echo "✅ Instância está executando"
                
                aws ec2 wait instance-status-ok --instance-ids ${env.INSTANCE_ID}
                echo "✅ Instância passou nos health checks"
            """
        }
    }
}

def applyTags() {
    echo "🏷️  Aplicando tags adicionais..."
    
    def accountId = params.AWS_ACCOUNT
    def credentialsId = getCredentialsId(accountId)
    
    withCredentials([aws(credentialsId: credentialsId)]) {
        def additionalTags = getEnvironmentTags()
        
        sh """
            aws ec2 create-tags \
                --resources ${env.INSTANCE_ID} \
                --tags ${additionalTags}
        """
        
        echo "✅ Tags aplicadas com sucesso"
    }
}

def getEnvironmentTags() {
    def currentDate = new Date().format('yyyy-MM-dd HH:mm:ss')
    def tags = [
        "Key=Owner,Value=DevOps-Team",
        "Key=Project,Value=lab-jenkins",
        "Key=CreatedDate,Value=${currentDate}",
        "Key=JenkinsJob,Value=${env.JOB_NAME}",
        "Key=JenkinsBuild,Value=${env.BUILD_URL}"
    ]
    
    // Tags específicas por ambiente
    switch(params.ENVIRONMENT) {
        case 'dev':
            tags.addAll([
                "Key=CostCenter,Value=Development",
                "Key=AutoShutdown,Value=true",
                "Key=BackupRequired,Value=false"
            ])
            break
        case 'staging':
            tags.addAll([
                "Key=CostCenter,Value=Testing",
                "Key=AutoShutdown,Value=false",
                "Key=BackupRequired,Value=true"
            ])
            break
        case 'prod':
            tags.addAll([
                "Key=CostCenter,Value=Production",
                "Key=AutoShutdown,Value=false",
                "Key=BackupRequired,Value=true",
                "Key=Compliance,Value=required"
            ])
            break
    }
    
    return tags.join(' ')
}

def displayInstanceInfo() {
    echo "📋 Exibindo informações da instância..."
    
    def accountId = params.AWS_ACCOUNT
    def credentialsId = getCredentialsId(accountId)
    
    withCredentials([aws(credentialsId: credentialsId)]) {
        def instanceInfo = sh(
            script: """
                aws ec2 describe-instances \
                    --instance-ids ${env.INSTANCE_ID} \
                    --query 'Reservations[0].Instances[0].{
                        InstanceId: InstanceId,
                        State: State.Name,
                        InstanceType: InstanceType,
                        PublicIpAddress: PublicIpAddress,
                        PrivateIpAddress: PrivateIpAddress,
                        KeyName: KeyName,
                        LaunchTime: LaunchTime
                    }' \
                    --output table
            """,
            returnStdout: true
        )
        
        echo "📊 INFORMAÇÕES DA INSTÂNCIA EC2:"
        echo "=================================="
        echo instanceInfo
        echo "=================================="
        echo "🌍 Ambiente: ${params.ENVIRONMENT}"
        echo "🏢 Conta AWS: ${params.AWS_ACCOUNT}"
        echo "🌎 Região: ${params.AWS_REGION}"
        echo "📝 Nome: ${env.INSTANCE_FULL_NAME}"
        echo "🆔 Instance ID: ${env.INSTANCE_ID}"
        echo "=================================="
    }
}

def sendSuccessNotification() {
    // Aqui você pode integrar com Slack, Teams, email, etc.
    echo "📧 Enviando notificação de sucesso..."
    
    def message = """
    ✅ SUCESSO: Instância EC2 criada!
    
    📋 Detalhes:
    • Nome: ${env.INSTANCE_FULL_NAME}
    • Instance ID: ${env.INSTANCE_ID}
    • Ambiente: ${params.ENVIRONMENT}
    • Tipo: ${params.INSTANCE_TYPE}
    • Região: ${params.AWS_REGION}
    • Build: ${env.BUILD_URL}
    
    🎉 A instância está pronta para uso!
    """
    
    echo message
}

def sendFailureNotification() {
    // Aqui você pode integrar com Slack, Teams, email, etc.
    echo "📧 Enviando notificação de falha..."
    
    def message = """
    ❌ FALHA: Erro na criação da instância EC2!
    
    📋 Detalhes:
    • Nome tentado: ${env.INSTANCE_FULL_NAME}
    • Ambiente: ${params.ENVIRONMENT}
    • Tipo: ${params.INSTANCE_TYPE}
    • Região: ${params.AWS_REGION}
    • Build: ${env.BUILD_URL}
    
    🔍 Verifique os logs para mais detalhes.
    """
    
    echo message
}