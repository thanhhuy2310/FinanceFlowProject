import { useQuery } from "@tanstack/react-query";
import { Building2, Search } from "lucide-react";
import { useState } from "react";

import { financeApi } from "../api/finance";
import { PageHeader } from "../components/ui/PageHeader";
import { EmptyState, ErrorState, TableSkeleton } from "../components/ui/States";

const PAGE_DESCRIPTION = "Financial institutions and wallets available when adding an account.";

export function ProvidersPage() {
  const [term, setTerm] = useState("");

  // Query
  const providers = useQuery({
    queryKey: ["providers"],
    queryFn: financeApi.providers,
    placeholderData: [],
  });

  // Derived values
  const items = (providers.data ?? []).filter((provider) =>
    provider.name.toLowerCase().includes(term.toLowerCase()),
  );

  if (providers.isLoading) {
    return (
      <>
        <PageHeader title="Providers" description={PAGE_DESCRIPTION} />
        <TableSkeleton />
      </>
    );
  }

  if (providers.isError) {
    return (
      <>
        <PageHeader title="Providers" description={PAGE_DESCRIPTION} />
        <ErrorState retry={() => providers.refetch()} />
      </>
    );
  }

  return (
    <>
      <PageHeader title="Providers" description={PAGE_DESCRIPTION} />

      <label className="search-field standalone-search">
        <Search size={17} />
        <span className="sr-only">Search providers</span>
        <input
          className="input"
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          placeholder="Search providers"
        />
      </label>

      {items.length === 0 ? (
        <EmptyState
          title="No providers found"
          description={
            term ? "Try a different search term." : "Providers will appear here once added by an administrator."
          }
        />
      ) : (
        <section className="provider-grid">
          {items.map((provider) => (
            <article className="card provider-card" key={provider.id}>
              {provider.logoUrl ? (
                <img className="provider-logo" src={provider.logoUrl} alt={`${provider.name} logo`} />
              ) : (
                <span className="provider-logo provider-fallback">
                  <Building2 size={20} />
                </span>
              )}
              <strong>{provider.name}</strong>
            </article>
          ))}
        </section>
      )}
    </>
  );
}
