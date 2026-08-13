import React from 'react';

export const TaskSection: React.FC<{
    title: string;
    subtitle: string;
    badge?: React.ReactNode;
    children: React.ReactNode;
}> = ({ title, subtitle, badge, children }) => (
    <div className="bg-white shadow sm:rounded-lg overflow-hidden border border-gray-200">
        <div className="px-4 py-5 sm:px-6 border-b border-gray-200 flex items-center justify-between">
            <div>
                <h3 className="text-lg leading-6 font-medium text-gray-900">{title}</h3>
                <p className="mt-1 text-sm text-gray-500">{subtitle}</p>
            </div>
            {badge}
        </div>
        <div className="divide-y divide-gray-200">{children}</div>
    </div>
);

export const TaskEmptyRow: React.FC<{ text: string }> = ({ text }) => (
    <p className="px-4 py-6 text-sm text-gray-500 text-center">{text}</p>
);
