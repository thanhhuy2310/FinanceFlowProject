import { useQuery } from "@tanstack/react-query";
import { Building2, Search } from "lucide-react";
import { useState } from "react";
import { financeApi } from "../api/finance";
import { EmptyState, ErrorState, TableSkeleton } from "../components/ui/States";
import { PageHeader } from "../components/ui/PageHeader";

export function ProvidersPage() {
  const providers = useQuery({ queryKey: ["providers"], queryFn: financeApi.providers, initialData: [] });
  const [term, setTerm] = useState("");
  const description = "Financial institutions and wallets available when adding an account.";
  if (providers.isLoading) return <><PageHeader title="Providers" description={description}/><TableSkeleton/></>;
  if (providers.isError) return <><PageHeader title="Providers" description={description}/><ErrorState retry={() => providers.refetch()}/></>;
  const items = providers.data.filter((provider) => provider.name.toLowerCase().includes(term.toLowerCase()));
  return <><PageHeader title="Providers" description={description}/><label className="search-field standalone-search"><Search size={17}/><span className="sr-only">Search providers</span><input className="input" value={term} onChange={(event) => setTerm(event.target.value)} placeholder="Search providers"/></label>{items.length === 0 ? <EmptyState title="No providers found" description="Try a different search term."/> : <section className="provider-grid">{items.map((provider) => <article className="card provider-card" key={provider.id}>{provider.logoUrl ? <img className="provider-logo" src={provider.logoUrl} alt=""/> : <span className="provider-logo provider-fallback"><Building2 size={20}/></span>}<strong>{provider.name}</strong></article>)}</section>}</>;
}
