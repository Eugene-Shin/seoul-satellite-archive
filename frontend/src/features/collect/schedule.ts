import cronstrue from "cronstrue/i18n";

// 수집 일정. 흔한 반복은 골라서 만들고, 나머지는 Spring cron(초 분 시 일 월 요일)을 직접 쓴다.
// cron은 서버가 한국 시간으로 해석한다
export const SCHEDULE_MODES = ["daily", "weekly", "hourly", "custom"] as const;
export type ScheduleMode = (typeof SCHEDULE_MODES)[number];

export const SCHEDULE_MODE_NAMES: Record<ScheduleMode, string> = {
    daily: "매일",
    weekly: "매주",
    hourly: "N시간마다",
    custom: "직접 입력",
};

export const WEEKDAYS = [
    { code: "MON", label: "월" },
    { code: "TUE", label: "화" },
    { code: "WED", label: "수" },
    { code: "THU", label: "목" },
    { code: "FRI", label: "금" },
    { code: "SAT", label: "토" },
    { code: "SUN", label: "일" },
] as const;
export type Weekday = (typeof WEEKDAYS)[number]["code"];

// 하루를 나눠떨어지게 해서 매일 같은 시각에 돈다
export const INTERVAL_HOURS = [1, 2, 3, 4, 6, 8, 12] as const;

// 방식마다 필요한 칸만 둔다. time은 HH:mm, N시간마다는 정각에 돈다
export type Schedule =
    | { mode: "daily"; time: string }
    | { mode: "weekly"; time: string; weekdays: Weekday[] }
    | { mode: "hourly"; intervalHours: number }
    | { mode: "custom"; cron: string };

const DEFAULT_TIME = "03:00";

export const DEFAULT_SCHEDULE: Schedule = { mode: "daily", time: DEFAULT_TIME };

// 방식을 바꾸면 그 방식의 기본값으로 채운다. 시각은 이어받고, 직접 입력은 지금 일정의 cron으로 시작한다
export function changeMode(current: Schedule, mode: ScheduleMode): Schedule {
    const time = "time" in current ? current.time : DEFAULT_TIME;
    switch (mode) {
        case "daily":
            return { mode, time };
        case "weekly":
            return { mode, time, weekdays: ["MON"] };
        case "hourly":
            return { mode, intervalHours: 6 };
        case "custom":
            return { mode, cron: toCron(current) };
    }
}

export function toCron(schedule: Schedule): string {
    switch (schedule.mode) {
        case "custom":
            return schedule.cron.trim();
        case "hourly":
            return `0 0 */${schedule.intervalHours} * * *`;
        case "daily":
            return `${timeFields(schedule.time)} * * *`;
        case "weekly":
            return `${timeFields(schedule.time)} * * ${weekdayList(schedule.weekdays)}`;
    }
}

function timeFields(time: string): string {
    const [hour, minute] = time.split(":").map(Number);
    return `0 ${minute} ${hour}`;
}

// 요일은 월요일부터 순서대로 쓴다
function weekdayList(weekdays: Weekday[]): string {
    return WEEKDAYS.filter((day) => weekdays.includes(day.code))
        .map((day) => day.code)
        .join(",");
}

const DAILY_OR_WEEKLY = /^0 (\d{1,2}) (\d{1,2}) \* \* (\*|[A-Z,]+)$/;
const HOURLY = /^0 0 \*\/(\d{1,2}) \* \* \*$/;

// 저장된 cron을 고를 수 있는 모양으로 풀고, 아니면 직접 입력으로 연다
export function fromCron(cron: string): Schedule {
    const normalized = cron.trim().replace(/\s+/g, " ");
    const custom: Schedule = { mode: "custom", cron };

    const hourly = HOURLY.exec(normalized);
    if (hourly) {
        const intervalHours = Number(hourly[1]);
        return INTERVAL_HOURS.some((hours) => hours === intervalHours)
            ? { mode: "hourly", intervalHours }
            : custom;
    }

    const match = DAILY_OR_WEEKLY.exec(normalized);
    if (!match) {
        return custom;
    }
    const minute = Number(match[1]);
    const hour = Number(match[2]);
    // 정규식은 두 자리 숫자를 다 받아서, 시각 입력칸에 넣을 수 없는 값은 직접 입력으로 둔다
    if (minute > 59 || hour > 23) {
        return custom;
    }
    const time = `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
    if (match[3] === "*") {
        return { mode: "daily", time };
    }
    const codes = match[3].split(",");
    const weekdays = WEEKDAYS.filter((day) => codes.includes(day.code)).map(
        (day) => day.code,
    );
    return weekdays.length === codes.length
        ? { mode: "weekly", time, weekdays }
        : custom;
}

// 사람이 읽는 설명. 해석할 수 없으면 null(형식 검사는 서버가 한다)
export function describeCron(cron: string): string | null {
    const parts = cron.trim().split(/\s+/);
    if (parts.length !== 6) {
        return null;
    }
    let text: string;
    try {
        text = cronstrue.toString(cron.trim(), {
            locale: "ko",
            use24HourTimeFormat: true,
        });
    } catch {
        return null;
    }
    // 한국어 번역은 날짜 조건이 없으면 "매일"을 생략한다
    const [, , hour, day, month, weekday] = parts;
    const everyDay =
        /^\d+$/.test(hour) && day === "*" && month === "*" && weekday === "*";
    return `${everyDay ? "매일 " : ""}${text} (KST)`;
}
