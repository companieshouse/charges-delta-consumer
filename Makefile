artifact_name       := charges-delta-consumer
version             := unversioned

# Create help from comments in Makefile
help:
	@printf "%-20s %s\n" "Target" "Description"
	@printf "%-20s %s\n" "------" "-----------"
	@make -pqR : 2>/dev/null \
        | awk -v RS= -F: '{if ($$1 !~ "^[#.]") {print $$1}}' \
        | sort \
        | egrep -v -e '^[^[:alnum:]]' -e '^$@$$' \
        | xargs -I _ sh -c 'printf "%-20s " _; make _ -nB | (grep -i "^# Help:" || echo "") | tail -1 | sed "s/^# Help: //g"'

.PHONY: all
all:
	@# Help: Calls methods required to build a locally runnable version, typically the build target
	mvn clean install

.PHONY: clean
clean:
	@# Help: Reset repo to pre-build state (i.e. a clean checkout state)
	mvn clean
	rm -f ./$(artifact_name).jar
	rm -f ./$(artifact_name)-*.zip
	rm -rf ./build-*
	rm -rf ./build.log-*

.PHONY: build
build:
	@# Help: Pull down any dependencies and compile code into an executable if required
	$(info Setting version: $(version))
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	$(info Packing version: $(version))
	mvn package -Dmaven.test.skip=true
	cp ./target/$(artifact_name)-$(version).jar ./$(artifact_name).jar

.PHONY: test
test: test-integration test-unit
	@# Help: Run all test-* targets (convenience method for developers)

.PHONY: test-unit
test-unit:
	@# Help: Run unit tests
	mvn test -Dskip.integration.tests=true

.PHONY: test-integration
test-integration:
	@# Help: Run integration tests
	mvn integration-test -Dskip.unit.tests=true

.PHONY: run-local
run-local:
	@# Help: Run springboot app locally
	mvn spring-boot:run

.PHONY: package
package:
	@# Help: Create a single versioned deployable package (i.e. jar, zip, tar, etc.). May be dependent on the build target being run before package
ifndef version
	$(error No version given. Aborting)
endif
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	$(info Packaging version: $(version))
	@test -s ./$(artifact_name).jar || { echo "ERROR: Service JAR not found"; exit 1; }
	$(eval tmpdir:=$(shell mktemp -d build-XXXXXXXXXX))
	cp ./$(artifact_name).jar $(tmpdir)/$(artifact_name).jar
	cd $(tmpdir); zip -r ../$(artifact_name)-$(version).zip *
	rm -rf $(tmpdir)

.PHONY: dist
dist: clean build package

.PHONY: sonar-pr-analysis
sonar-pr-analysis:
	@# Help: Run sonar scan on a PR
	mvn verify sonar:sonar -P sonar-pr-analysis

.PHONY: sonar
sonar:
	@# Help: Run sonar scan
	mvn verify sonar:sonar

.PHONY: deps
deps:
	@# Help: Install dependencies
	brew install kafka

.PHONY: docker/kafka
docker/kafka: docker/kafka-start docker/kafka-create-topics
	@# Help: Run kafka and create topics within docker

.PHONY: docker/kafka-start
docker/kafka-start:
	@# Help: Run kafka within docker
	tilt up
	sleep 15

.PHONY: docker/kafka-create-topics
docker/kafka-create-topics:
	@# Help: Create kafka topics within docker
	@kafka-topics --create   --bootstrap-server localhost:29092   --replication-factor 1 --partitions 1   --topic charges-delta
	@kafka-topics --create   --bootstrap-server localhost:29092   --replication-factor 1 --partitions 1   --topic charges-delta-retry
	@kafka-topics --create   --bootstrap-server localhost:29092   --replication-factor 1 --partitions 1   --topic charges-delta-error
	@printf "\nKafka Topics:\n\n"
	@kafka-topics --list --bootstrap-server localhost:29092

.PHONY: docker/kafka-stop
docker/kafka-stop:
	@# Help: Stop kafka within docker
	tilt down

.PHONY: lint
lint: lint/docker-compose sonar
	@# Help: Run all lint/* targets and sonar

.PHONY: lint/docker-compose
lint/docker-compose:
	@# Help: Lint docker file
	docker-compose -f docker-compose.yml config