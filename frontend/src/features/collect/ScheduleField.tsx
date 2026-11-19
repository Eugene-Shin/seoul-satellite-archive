"use client";

import {
    changeMode,
    describeCron,
    INTERVAL_HOURS,
    type Schedule,
    SCHEDULE_MODE_NAMES,
    SCHEDULE_MODES,
    type ScheduleMode,
    toCron,
    type Weekday,
    WEEKDAYS,
} from "@/features/collect/schedule";
import { Field, FormSelect } from "@/shared/components/FormField";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";

const MODE_OPTIONS = SCHEDULE_MODES.map((mode) => ({
    value: mode,
    label: SCHEDULE_MODE_NAMES[mode],
}));

const INTERVAL_OPTIONS = INTERVAL_HOURS.map((hours) => ({
    value: hours,
    label: `${hours}시간`,
}));

const UNKNOWN_CRON =
    "설명할 수 없는 cron입니다. 저장할 때 서버가 형식을 확인합니다.";
const UNUSED_SCHEDULE = "정기 수집이 꺼져 있어 일정은 쓰이지 않습니다.";

type ScheduleFieldProps = {
    value: Schedule;
    invalid: boolean;
    // 정기 수집이 꺼져 있으면 흐리게 두고 값은 지우지 않는다(다시 켜면 그대로 쓴다)
    disabled: boolean;
    onChange: (schedule: Schedule) => void;
};

export function ScheduleField({
    value,
    invalid,
    disabled,
    onChange,
}: ScheduleFieldProps) {
    const description = disabled
        ? UNUSED_SCHEDULE
        : (describeCron(toCron(value)) ?? UNKNOWN_CRON);

    // 선택지에 "선택 안 함"이 없어서 null은 오지 않지만 FormSelect 타입이 허용한다
    function selectMode(mode: ScheduleMode | null) {
        if (mode !== null) {
            onChange(changeMode(value, mode));
        }
    }

    return (
        <div className="flex flex-col gap-3">
            {/* fieldset disabled는 안의 입력칸·선택 버튼을 한 번에 막는다. contents라 배치에는 영향이 없다 */}
            <fieldset disabled={disabled} className="contents">
                <FormSelect
                    field="schedule"
                    label="반복"
                    required
                    invalid={invalid}
                    options={MODE_OPTIONS}
                    value={value.mode}
                    onChange={selectMode}
                />
                <ScheduleDetail value={value} onChange={onChange} />
            </fieldset>
            <p className="text-xs text-muted-foreground">{description}</p>
        </div>
    );
}

type ScheduleDetailProps = Pick<ScheduleFieldProps, "value" | "onChange">;

// 고른 방식에 필요한 칸만 보여 준다
function ScheduleDetail({ value, onChange }: ScheduleDetailProps) {
    switch (value.mode) {
        case "daily":
            return (
                <TimeField
                    time={value.time}
                    onChange={(time) => onChange({ ...value, time })}
                />
            );
        case "weekly":
            return (
                <div className="flex flex-col gap-3">
                    <WeekdayPicker
                        weekdays={value.weekdays}
                        onChange={(weekdays) =>
                            onChange({ ...value, weekdays })
                        }
                    />
                    <TimeField
                        time={value.time}
                        onChange={(time) => onChange({ ...value, time })}
                    />
                </div>
            );
        case "hourly": {
            // 선택지에 "선택 안 함"이 없어서 null은 오지 않지만 FormSelect 타입이 허용한다
            const selectInterval = (hours: number | null) => {
                if (hours !== null) {
                    onChange({ ...value, intervalHours: hours });
                }
            };
            return (
                <FormSelect
                    label="반복 간격"
                    options={INTERVAL_OPTIONS}
                    value={value.intervalHours}
                    onChange={selectInterval}
                />
            );
        }
        case "custom":
            return (
                <Field label="cron" hint="초 분 시 일 월 요일 (KST)">
                    <Input
                        className="font-mono"
                        value={value.cron}
                        onChange={(e) =>
                            onChange({ ...value, cron: e.target.value })
                        }
                    />
                </Field>
            );
    }
}

type TimeFieldProps = {
    time: string;
    onChange: (time: string) => void;
};

function TimeField({ time, onChange }: TimeFieldProps) {
    return (
        <Field label="시각">
            <Input
                type="time"
                value={time}
                onChange={(e) => onChange(e.target.value)}
            />
        </Field>
    );
}

type WeekdayPickerProps = {
    weekdays: Weekday[];
    onChange: (weekdays: Weekday[]) => void;
};

function WeekdayPicker({ weekdays, onChange }: WeekdayPickerProps) {
    function toggle(day: Weekday) {
        onChange(
            weekdays.includes(day)
                ? weekdays.filter((selected) => selected !== day)
                : [...weekdays, day],
        );
    }

    return (
        <div className="flex flex-col gap-2">
            <span className="text-sm font-medium">요일</span>
            <div className="flex flex-wrap gap-1">
                {WEEKDAYS.map((day) => (
                    <WeekdayToggle
                        key={day.code}
                        label={day.label}
                        selected={weekdays.includes(day.code)}
                        onToggle={() => toggle(day.code)}
                    />
                ))}
            </div>
        </div>
    );
}

type WeekdayToggleProps = {
    label: string;
    selected: boolean;
    onToggle: () => void;
};

function WeekdayToggle({ label, selected, onToggle }: WeekdayToggleProps) {
    const variant = selected ? "default" : "outline";

    return (
        <Button
            type="button"
            size="sm"
            variant={variant}
            aria-pressed={selected}
            onClick={onToggle}
        >
            {label}
        </Button>
    );
}
