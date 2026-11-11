export type ImportAction = 'CREATE' | 'UPDATE';
export type ImportLevel = 'ERROR' | 'WARNING';

export interface ImportItem {
  row: number;
  sku: string;
  name: string;
  category: string;
  size: string;
  price: number;
  stock: number;
  currentStock: number;
  action: ImportAction;
}

export interface ImportIssue {
  row: number;
  level: ImportLevel;
  message: string;
}

export interface ImportCategory {
  name: string;
  count: number;
  isNew: boolean;
}

export interface ImportResult {
  dryRun: boolean;
  fileName: string;
  totalRows: number;
  created: number;
  updated: number;
  skipped: number;
  categories: ImportCategory[];
  items: ImportItem[];
  issues: ImportIssue[];
}
