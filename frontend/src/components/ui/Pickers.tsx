import { Tag, X } from "lucide-react";

import { CATEGORY_COLORS, CATEGORY_ICONS, ICON_MAP } from "./pickerOptions";

interface CategoryIconProps {
  name?: string | null;
  size?: number;
}

export function CategoryIcon({ name, size = 16 }: CategoryIconProps) {
  const Icon = (name && ICON_MAP[name]) || Tag;

  return <Icon size={size} />;
}

interface IconPickerProps {
  value: string;
  onChange: (name: string) => void;
  labelledBy: string;
}

export function IconPicker({ value, onChange, labelledBy }: IconPickerProps) {
  return (
    <div className="icon-picker" role="group" aria-labelledby={labelledBy}>
      {CATEGORY_ICONS.map((name) => {
        const Icon = ICON_MAP[name];
        const selected = value === name;

        return (
          <button
            key={name}
            type="button"
            className={`icon-option${selected ? " selected" : ""}`}
            aria-label={`Icon ${name}`}
            aria-pressed={selected}
            title={name}
            onClick={() => onChange(selected ? "" : name)}
          >
            <Icon size={17} />
          </button>
        );
      })}
    </div>
  );
}

interface ColorPickerProps {
  value: string;
  onChange: (color: string) => void;
}

export function ColorPicker({ value, onChange }: ColorPickerProps) {
  const selected = value?.toLowerCase();

  return (
    <div className="color-picker" role="group" aria-label="Color">
      {CATEGORY_COLORS.map((color) => {
        const isSelected = selected === color;

        return (
          <button
            key={color}
            type="button"
            className={`color-option${isSelected ? " selected" : ""}`}
            style={{ background: color }}
            aria-label={`Color ${color}`}
            aria-pressed={isSelected}
            title={color}
            onClick={() => onChange(isSelected ? "" : color)}
          />
        );
      })}

      <label className="color-custom" title="Custom color">
        <span className="sr-only">Custom color</span>
        <input
          type="color"
          value={value || "#3B82F6"}
          onChange={(event) => onChange(event.target.value)}
        />
      </label>

      {value && (
        <button
          type="button"
          className="icon-button"
          aria-label="Clear color"
          onClick={() => onChange("")}
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}
