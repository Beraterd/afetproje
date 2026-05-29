import React, { ReactNode } from 'react';
import { EmptyState } from './EmptyState';
import { Pagination } from './Pagination';
import { LoadingSpinner } from './LoadingSpinner';

export interface ColumnDef<T> {
    header: string;
    accessor?: keyof T;
    render?: (row: T) => ReactNode;
}

interface DataTableProps<T> {
    columns: ColumnDef<T>[];
    data: T[];
    isLoading?: boolean;
    emptyMessage?: string;
    pagination?: {
        page: number;
        totalPages: number;
    };
    onPageChange?: (page: number) => void;
}

export function DataTable<T>({
    columns,
    data,
    isLoading,
    emptyMessage = 'No data available',
    pagination,
    onPageChange,
}: DataTableProps<T>) {
    if (isLoading) {
        return (
            <div className="w-full bg-white shadow rounded-lg overflow-hidden">
                <div className="px-6 py-12 flex justify-center">
                    <LoadingSpinner size="md" />
                </div>
            </div>
        );
    }

    if (!data || data.length === 0) {
        return (
            <div className="w-full">
                <EmptyState title={emptyMessage} className="bg-white shadow" />
            </div>
        );
    }

    return (
        <div className="w-full bg-white shadow rounded-lg overflow-hidden flex flex-col">
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            {columns.map((col, idx) => (
                                <th
                                    key={idx}
                                    scope="col"
                                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap"
                                >
                                    {col.header}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {data.map((row, rowIdx) => (
                            <tr key={rowIdx} className="hover:bg-gray-50 transition-colors">
                                {columns.map((col, colIdx) => (
                                    <td key={colIdx} className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 overflow-hidden text-ellipsis max-w-[250px]">
                                        {col.render
                                            ? col.render(row)
                                            : col.accessor
                                                ? (row[col.accessor] as unknown as ReactNode)
                                                : null}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {pagination && onPageChange && (
                <Pagination
                    page={pagination.page}
                    totalPages={pagination.totalPages}
                    onPageChange={onPageChange}
                />
            )}
        </div>
    );
}
