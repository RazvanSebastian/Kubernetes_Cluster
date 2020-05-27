def tag="razvan95"

podTemplate(containers: [
    containerTemplate(name: "maven", image: "maven:3.3.9-jdk-8-alpine", ttyEnabled: true, command: "cat"),
    containerTemplate(name: "docker", image: "docker", command: "cat", ttyEnabled: true),
    ],
    volumes: [
        hostPathVolume(hostPath: "/var/run/docker.sock", mountPath: "/var/run/docker.sock")
    ],
){
    node(POD_LABEL) {
        stage("maven container stage") {
            git "https://github.com/RazvanSebastian/SpringBoot_HelloWorld.git"
            container("maven") {
                stage("Build a Maven project") {
                    sh "mvn -B clean install -DskipTests"
                }
            }
        }
        stage("Docker container stage"){
            container("docker"){
                stage("Build image"){
                    imageBuild("spring-rest-app", "latest")
                }
                stage("Push image"){
                    withCredentials([usernamePassword(credentialsId: "dockerhub", usernameVariable: "username", passwordVariable: "password")]) {
			            pushToImage("spring-rest-app", "latest", USERNAME, PASSWORD)
                    }
                }
            }
        }
        stage("Kubernetes deploy"){
			sshagent (credentials: ['sshAgent']) {
				sh 'ssh -o StrictHostKeyChecking=no root@192.168.1.10 /home/user/ci-di-demo/deploy.sh'
			}
		}
    }
}

def imageBuild(containerName, tag){
    sh "docker build -t ${containerName}:${tag} ."
}

def pushToImage(containerName, tag, dockerUser, dockerPassword){
    sh "docker login -u ${dockerUser} -p ${dockerPassword}"
    sh "docker tag ${containerName}:${tag} ${dockerUser}/${containerName}:${tag}"
    sh "docker push ${dockerUser}/${containerName}:${tag}"
    echo "Image push complete"
}