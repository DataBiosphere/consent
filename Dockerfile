# Builder
FROM maven:3.9.11-eclipse-temurin-25 AS build

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY .git /usr/src/app/.git
COPY pom.xml /usr/src/app/pom.xml
COPY src /usr/src/app/src

RUN mvn clean package -Dmaven.test.skip=true --no-transfer-progress

# Runtime — FIPS 140-3 hardened
# FROM us.gcr.io/broad-dsp-gcr-public/base/jre:25-jre
FROM dhi.io/eclipse-temurin:25 AS runtime-base

# Security patcher — dhi.io/eclipse-temurin:25 is distroless (no shell), so use the
# maven image's shell to patch java.security extracted from the runtime image.
#
# Register bc-fips as the primary JCE security provider in the JVM's security
# configuration. Existing providers are renumbered (N → N+1) so non-crypto
# subsystems (XML, GSSAPI, SASL) retain their fallback providers.
# The TLSv1/1.1 guard is belt-and-suspenders: JDK 25 already disables them,
# but the explicit sed ensures they stay out if the base image is upgraded.
#
# Note: OS-level OpenSSL FIPS is not configured here. Ubuntu's standard package
# repositories do not ship the OpenSSL FIPS provider module (fips.so); it requires
# an Ubuntu Pro subscription with the ubuntu-fips kernel and fips-updates channel.
# This is not a coverage gap for this application: consent is a pure-Java service
# with no native crypto code. Conscrypt is excluded from all dependencies, and every
# cryptographic operation goes through JCE → bc-fips (FIPS 140-3 targeted).
FROM maven:3.9.11-eclipse-temurin-25 AS security-patcher
COPY --from=runtime-base /opt/java/openjdk/25-jre/conf/security/java.security /java.security
RUN i=20 \
    && while [ "$i" -ge 1 ]; do \
         j=$((i+1)); \
         sed -i "s/^security\\.provider\\.${i}=/security.provider.${j}=/" /java.security; \
         i=$((i-1)); \
       done \
    && sed -i '/^security\.provider\.2=/i security.provider.1=org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider' \
         /java.security \
    && if ! grep -q 'TLSv1,' /java.security; then \
         sed -i 's/^jdk\.tls\.disabledAlgorithms=/jdk.tls.disabledAlgorithms=TLSv1, TLSv1.1, /' /java.security; \
       fi

FROM dhi.io/eclipse-temurin:25

# Copy the bc-fips 2.0.0 jar (FIPS 140-3 targeted) from the Maven local cache
# populated during the build stage into a stable location in the runtime image.
COPY --from=build \
     /root/.m2/repository/org/bouncycastle/bc-fips/2.0.0/bc-fips-2.0.0.jar \
     /opt/bc-fips-2.0.0.jar

COPY --from=security-patcher /java.security /opt/java/openjdk/25-jre/conf/security/java.security

# Add bc-fips to the JVM boot classpath so the security provider class is resolvable
# during JVM initialisation before any application class is loaded.
# approved_only=true causes BouncyCastle to reject any non-FIPS algorithm at runtime.
ENV JAVA_TOOL_OPTIONS="-Xbootclasspath/a:/opt/bc-fips-2.0.0.jar -Dorg.bouncycastle.fips.approved_only=true"

COPY --from=build /usr/src/app/target/consent.jar /opt/consent.jar
