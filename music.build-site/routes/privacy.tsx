/** Privacy Policy — required by GDPR/DSGVO. */
export default function PrivacyPage() {
  return (
    <>
      <h1 class="text-3xl font-bold text-[var(--denote-text)] mb-2 font-[var(--denote-font-heading)]">
        Privacy Policy
      </h1>
      <p class="text-sm text-[var(--denote-text-muted)] mb-8">
        Last updated: 2026-04-19
      </p>

      <div class="prose prose-lg text-[var(--denote-text-secondary)] space-y-8">
        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Overview
          </h2>
          <p>
            This website is a documentation site for the open-source{" "}
            <strong>music.build</strong>{" "}
            project. We take your privacy seriously and process as little
            personal data as possible.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Responsible Party
          </h2>
          <p>
            The party responsible for data processing on this website is listed
            in our{" "}
            <a
              href="/impressum"
              class="text-[var(--denote-primary-text)] hover:underline"
            >
              Impressum
            </a>. For privacy-related inquiries, contact us at{" "}
            <a
              href="mailto:privacy@music.build"
              class="text-[var(--denote-primary-text)] hover:underline"
            >
              privacy@music.build
            </a>.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Hosting
          </h2>
          <p>
            This website is hosted on{" "}
            <strong>Deno Deploy (Deno Land Inc.)</strong>, located on a global
            edge network (primarily US/EU). When you visit this site, your
            browser connects to their servers, which may log:
          </p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>IP address</li>
            <li>Date and time of the request</li>
            <li>Requested URL</li>
            <li>Browser type and version</li>
            <li>Referring URL</li>
          </ul>
          <p class="mt-3">
            This data is processed based on our legitimate interest in providing
            a secure and functional website (Art. 6(1)(f) GDPR). Server logs are
            retained by the hosting provider according to their own retention
            policies.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Analytics
          </h2>
          <p>
            This site uses server-side analytics to understand usage patterns.
            No cookies are placed on your device for analytics purposes, and no
            personal data is sent to third-party analytics services from your
            browser.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Cookies
          </h2>
          <p>
            This website does not use cookies for tracking or advertising.
            Functional cookies may be used to remember your preferences (such as
            dark mode) using your browser's local storage. These do not
            constitute personal data processing under GDPR.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Third-Party Services
          </h2>
          <p>
            This site self-hosts all resources (styles, scripts, fonts). No
            advertising networks, tracking pixels, or Google services are
            loaded.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Your Rights
          </h2>
          <p>Under GDPR, you have the right to:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>
              <strong>Access</strong>{" "}
              — request information about your processed data (Art. 15)
            </li>
            <li>
              <strong>Rectification</strong> — correct inaccurate data (Art. 16)
            </li>
            <li>
              <strong>Erasure</strong> — request deletion of your data (Art. 17)
            </li>
            <li>
              <strong>Restriction</strong> — restrict processing (Art. 18)
            </li>
            <li>
              <strong>Data portability</strong>{" "}
              — receive your data in a portable format (Art. 20)
            </li>
            <li>
              <strong>Objection</strong>{" "}
              — object to processing based on legitimate interest (Art. 21)
            </li>
          </ul>
          <p class="mt-3">
            To exercise these rights, contact{" "}
            <a
              href="mailto:privacy@music.build"
              class="text-[var(--denote-primary-text)] hover:underline"
            >
              privacy@music.build
            </a>. You also have the right to lodge a complaint with a
            supervisory authority (in Germany: your state's
            Landesdatenschutzbeauftragter).
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            International Data Transfers
          </h2>
          <p>
            Our hosting provider operates servers that may process data outside
            the EU/EEA. Data transfers to the United States are covered by the
            EU-U.S. Data Privacy Framework. For other regions, Standard
            Contractual Clauses (SCCs) apply as required.
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Changes to This Policy
          </h2>
          <p>
            We may update this privacy policy from time to time. Changes will be
            posted on this page with an updated revision date.
          </p>
        </section>
      </div>
    </>
  );
}
