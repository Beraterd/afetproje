import React from 'react';
import { Badge } from '@/components/ui';
import { MapPin } from 'lucide-react';
import type { DamageAssessmentTaskResponse } from '@/types';
import { DAMAGE_LEVEL_COLOR } from './taskLabels';

export const PastDamageTaskRow: React.FC<{ task: DamageAssessmentTaskResponse }> = ({ task }) => (
    <div className="px-4 py-4 sm:px-6 flex items-center justify-between gap-4">
        <div className="min-w-0">
            <p className="text-sm font-medium text-gray-900 flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-gray-400 flex-shrink-0" />
                <span className="truncate">{task.address}</span>
            </p>
            <p className="text-xs text-gray-500 mt-0.5">
                {task.districtName} / {task.neighborhoodName}
            </p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
            <span className={`text-xs px-2 py-0.5 rounded-full ${DAMAGE_LEVEL_COLOR[task.damageLevel] || 'bg-gray-100 text-gray-600'}`}>
                {task.damageLevelLabel}
            </span>
            <Badge variant="neutral">{task.assignmentStatusLabel}</Badge>
        </div>
    </div>
);
