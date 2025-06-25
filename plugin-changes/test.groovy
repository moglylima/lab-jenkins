pipeline {
    agent any
    
    // Configuração básica
    options {
        disableConcurrentBuilds()
        timeout(time: 5, unit: 'MINUTES')
    }
    
    parameters {
        // Parâmetro principal
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'prod'],
            description: 'Selecione o ambiente'
        )
        
        // Parâmetro dinâmico 1: Regiões
        activeChoice(
            name: 'AWS_REGION',
            description: 'Região AWS',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        // Map de regiões por ambiente
                        def regions = [
                            'dev': ['us-east-1', 'sa-east-1'],
                            'prod': ['us-east-1', 'eu-west-1', 'ap-southeast-1']
                        ]
                        
                        // Retorna regiões baseadas no ambiente selecionado
                        return regions.get(params.ENVIRONMENT, ['Região não definida'])
                    ''',
                    fallbackScript: "return ['Erro: Ambiente não especificado']"
                ]
            ]
        )
        
        // Parâmetro dinâmico 2: Tipos de instância
        activeChoice(
            name: 'INSTANCE_TYPE',
            description: 'Tipo de instância EC2',
            choiceType: 'PT_SINGLE_SELECT',
            script: [
                $class: 'GroovyScript',
                script: [
                    script: '''
                        // Map de tipos por ambiente
                        def types = [
                            'dev': ['t3.micro', 't3.small'],
                            'prod': ['m5.large', 'm5.xlarge', 'c5.2xlarge']
                        ]
                        
                        // Retorna tipos baseados no ambiente selecionado
                        return types.get(params.ENVIRONMENT, ['Tipo não definido'])
                    ''',
                    fallbackScript: "return ['Erro: Ambiente não especificado']"
                ]
            ]
        )
    }
    
    stages {
        stage('Mostrar Seleções') {
            steps {
                script {
                    echo "Ambiente selecionado: ${params.ENVIRONMENT}"
                    echo "Região AWS: ${params.AWS_REGION}"
                    echo "Tipo de Instância: ${params.INSTANCE_TYPE}"
                    
                    // Verificar se os valores são válidos
                    if (params.AWS_REGION == 'Região não definida' || 
                        params.INSTANCE_TYPE == 'Tipo não definido') {
                        error("Valores inválidos selecionados!")
                    }
                }
            }
        }
        
        stage('Simular Criação') {
            steps {
                script {
                    echo "Simulando criação de instância..."
                    echo "🔧 Configurando região: ${params.AWS_REGION}"
                    echo "🖥️  Usando tipo: ${params.INSTANCE_TYPE}"
                    echo "✅ Simulação concluída com sucesso!"
                }
            }
        }
    }
    
    post {
        always {
            echo "Fim da execução"
        }
    }
}