"use client";

import { RotateCcwIcon } from "lucide-react";
import { useRef, useState } from "react";
import { toast } from "sonner";

import { AoiPreviewPopover } from "@/features/collect/AoiPreviewMap";
import {
    type CollectTargetField,
    type CollectTargetFormValues,
    emptyFormValues,
    isSameForm,
    missingFields,
    toFormValues,
} from "@/features/collect/collectTargetFormValues";
import {
    useCollectTargets,
    useDefaultAoi,
    useSaveCollectTarget,
} from "@/features/collect/queries";
import { ScheduleField } from "@/features/collect/ScheduleField";
import type { AoiPreset, CollectTarget } from "@/features/collect/types";
import { useCollections } from "@/features/image/queries";
import type { Collection } from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import {
    Field,
    focusField,
    FORM_DIALOG_CLASS,
    FormSelect,
} from "@/shared/components/FormField";
import { Button } from "@/shared/components/ui/button";
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/shared/components/ui/dialog";
import { Input } from "@/shared/components/ui/input";
import { Switch } from "@/shared/components/ui/switch";
import { Textarea } from "@/shared/components/ui/textarea";

// 등록할 때는 "new", 수정할 때는 대상, 닫혀 있으면 null
export type CollectTargetEditing = CollectTarget | "new" | null;

type CollectTargetFormDialogProps = {
    editing: CollectTargetEditing;
    onClose: () => void;
};

export function CollectTargetFormDialog({
    editing,
    onClose,
}: CollectTargetFormDialogProps) {
    const title = editing === "new" ? "수집 대상 추가" : "수집 대상 수정";

    return (
        // 여는 버튼이 없어서 onOpenChange는 닫을 때만 온다
        <Dialog open={editing !== null} onOpenChange={onClose}>
            <DialogContent className={FORM_DIALOG_CLASS}>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                </DialogHeader>
                <CollectTargetFormLoader editing={editing} onSaved={onClose} />
            </DialogContent>
        </Dialog>
    );
}

type CollectTargetFormLoaderProps = {
    editing: CollectTargetEditing;
    onSaved: () => void;
};

// 영역 기본값과 컬렉션 목록이 온 뒤에 폼을 그려서, 그 값을 폼 state의 초기값으로 쓴다
function CollectTargetFormLoader({
    editing,
    onSaved,
}: CollectTargetFormLoaderProps) {
    const defaultAoi = useDefaultAoi();
    const collections = useCollections();
    const targets = useCollectTargets();

    if (editing === null) {
        return null;
    }
    const error = defaultAoi.error ?? collections.error ?? targets.error;
    if (error) {
        return <ErrorMessage error={error} />;
    }
    if (!defaultAoi.data || !collections.data || !targets.data) {
        return (
            <p className="text-sm text-muted-foreground">
                불러오는 중입니다...
            </p>
        );
    }

    const target = editing === "new" ? null : editing;
    // 컬렉션마다 수집 대상은 하나라서, 등록할 때는 이미 대상이 있는 컬렉션을 뺀다
    const usedCollectionIds = targets.data.map(
        (existing) => existing.collection.collectionId,
    );
    const choosable = collections.data.filter(
        (collection) =>
            collection.collectable &&
            (target !== null ||
                !usedCollectionIds.includes(collection.collectionId)),
    );
    const initialValues = target
        ? toFormValues(target)
        : emptyFormValues(
              defaultAoi.data.aoi,
              choosable[0]?.collectionId ?? null,
          );

    return (
        <CollectTargetForm
            target={target}
            initialValues={initialValues}
            collections={choosable}
            defaultAoi={defaultAoi.data}
            onSaved={onSaved}
        />
    );
}

type CollectTargetFormProps = {
    target: CollectTarget | null;
    initialValues: CollectTargetFormValues;
    collections: Collection[];
    defaultAoi: AoiPreset;
    onSaved: () => void;
};

function CollectTargetForm({
    target,
    initialValues,
    collections,
    defaultAoi,
    onSaved,
}: CollectTargetFormProps) {
    const [form, setForm] = useState(initialValues);
    // 한 번 제출을 누른 뒤부터 빈 필수 칸을 표시한다
    const [submitted, setSubmitted] = useState(false);
    const aoiInputRef = useRef<HTMLTextAreaElement>(null);
    const [aoiFocused, setAoiFocused] = useState(false);
    const save = useSaveCollectTarget(target);

    const missing = submitted ? missingFields(form) : [];
    const unchanged = target !== null && isSameForm(form, initialValues);
    const submitLabel = target ? "저장" : "등록";
    const collectionOptions = collections.map((collection) => ({
        value: collection.collectionId,
        label: collection.title,
    }));
    const defaultAoiText = JSON.stringify(defaultAoi.aoi);
    const usingDefaultAoi = form.aoi === defaultAoiText;
    const aoiSource = usingDefaultAoi ? defaultAoi.source : null;

    function invalid(field: CollectTargetField): boolean {
        return missing.includes(field);
    }

    function change(changes: Partial<CollectTargetFormValues>) {
        setForm((current) => ({ ...current, ...changes }));
    }

    function selectCollection(collectionId: number | null) {
        change({ collectionId });
    }

    return (
        <form
            noValidate
            onSubmit={(event) => {
                event.preventDefault();
                setSubmitted(true);
                const [firstMissing] = missingFields(form);
                if (firstMissing) {
                    focusField(event.currentTarget, firstMissing);
                    return;
                }
                save.mutate(form, {
                    onSuccess: () => {
                        onSaved();
                        toast.success(`수집 대상을 ${submitLabel}했습니다.`);
                    },
                });
            }}
            className="flex min-h-0 flex-col gap-4"
        >
            {/* 입력칸만 스크롤해서 제목·닫기·버튼은 항상 보이게. 음수 여백은 초점 테두리가 잘리지 않게 */}
            <div className="-mx-4 flex min-h-0 flex-col gap-4 overflow-y-auto px-4 py-1">
                <div className="grid gap-4 sm:grid-cols-2">
                    <div className="flex flex-col gap-2">
                        <FormSelect
                            field="collectionId"
                            label="컬렉션"
                            required
                            invalid={invalid("collectionId")}
                            disabled={target !== null}
                            options={collectionOptions}
                            value={form.collectionId}
                            onChange={selectCollection}
                        />
                        {target === null && collections.length === 0 && (
                            <p className="text-xs text-muted-foreground">
                                수집할 수 있는 컬렉션마다 이미 수집 대상이
                                있습니다. 기존 대상의 영역에 지역을 추가하세요.
                            </p>
                        )}
                    </div>
                    <Field
                        field="lookbackDays"
                        label="재조회 기간 (일)"
                        required
                        invalid={invalid("lookbackDays")}
                        hint="1~31"
                    >
                        <Input
                            type="number"
                            min={1}
                            max={31}
                            value={form.lookbackDays}
                            onChange={(e) =>
                                change({ lookbackDays: e.target.value })
                            }
                        />
                    </Field>
                    <ScheduleField
                        value={form.schedule}
                        invalid={invalid("schedule")}
                        disabled={!form.enabled}
                        onChange={(schedule) => change({ schedule })}
                    />
                    <label className="flex items-center gap-2 self-start pt-8 text-sm font-medium">
                        <Switch
                            checked={form.enabled}
                            onCheckedChange={(enabled) => change({ enabled })}
                        />
                        정기 수집
                    </label>
                </div>
                <Field
                    field="aoi"
                    label="수집 영역"
                    required
                    invalid={invalid("aoi")}
                    hint="GeoJSON MultiPolygon"
                >
                    <Textarea
                        ref={aoiInputRef}
                        className="max-h-32 font-mono text-xs"
                        value={form.aoi}
                        onChange={(e) => change({ aoi: e.target.value })}
                        onFocus={() => setAoiFocused(true)}
                        onBlur={() => setAoiFocused(false)}
                    />
                </Field>
                <AoiPreviewPopover
                    anchor={aoiInputRef}
                    open={aoiFocused}
                    aoiText={form.aoi}
                    source={aoiSource}
                />
                <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-xs text-muted-foreground">
                        영역을 넓히면 새로 들어온 지역은 재조회 기간만큼만 새로
                        받게 됩니다.
                    </p>
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={usingDefaultAoi}
                        onClick={() => change({ aoi: defaultAoiText })}
                    >
                        <RotateCcwIcon />
                        {defaultAoi.name} 경계로 되돌리기
                    </Button>
                </div>
            </div>
            <ErrorMessage error={save.error} />
            <DialogFooter>
                <DialogClose render={<Button variant="outline" />}>
                    취소
                </DialogClose>
                <Button type="submit" disabled={save.isPending || unchanged}>
                    {save.isPending ? `${submitLabel} 중...` : submitLabel}
                </Button>
            </DialogFooter>
        </form>
    );
}
