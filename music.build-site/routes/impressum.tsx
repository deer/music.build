/** Impressum (Legal Notice) — required by German law (§ 5 DDG). */
export default function ImpressumPage() {
  return (
    <>
      <h1 class="text-3xl font-bold text-[var(--denote-text)] mb-8 font-[var(--denote-font-heading)]">
        Impressum
      </h1>

      <div class="prose prose-lg text-[var(--denote-text-secondary)] space-y-6">
        <hr class="border-[var(--denote-border)]" />

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Angaben gemäß § 5 DDG
          </h2>
          <p>
            Reed von Redwitz<br />
            c/o COCENTER<br />
            Koppoldstr. 1<br />
            86551 Aichach
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Kontakt
          </h2>
          <p>
            E-Mail:{" "}
            <a
              href="mailto:legal@music.build"
              class="text-[var(--denote-primary-text)] hover:underline"
            >
              legal@music.build
            </a>
          </p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-[var(--denote-text)] mb-3">
            Haftungsausschluss
          </h2>

          <h3 class="text-lg font-medium text-[var(--denote-text)] mb-2">
            Haftung für Inhalte
          </h3>
          <p>
            Die Inhalte unserer Seiten wurden mit größter Sorgfalt erstellt. Für
            die Richtigkeit, Vollständigkeit und Aktualität der Inhalte können
            wir jedoch keine Gewähr übernehmen. Als Diensteanbieter sind wir
            gemäß § 7 Abs. 1 DDG für eigene Inhalte auf diesen Seiten nach den
            allgemeinen Gesetzen verantwortlich. Nach §§ 8 bis 10 DDG sind wir
            als Diensteanbieter jedoch nicht verpflichtet, übermittelte oder
            gespeicherte fremde Informationen zu überwachen.
          </p>

          <h3 class="text-lg font-medium text-[var(--denote-text)] mb-2 mt-4">
            Haftung für Links
          </h3>
          <p>
            Unser Angebot enthält Links zu externen Webseiten Dritter, auf deren
            Inhalte wir keinen Einfluss haben. Für die Inhalte der verlinkten
            Seiten ist stets der jeweilige Anbieter verantwortlich.
          </p>
        </section>
      </div>
    </>
  );
}
