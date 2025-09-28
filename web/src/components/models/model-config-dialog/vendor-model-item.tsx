'use client'

import { Link, Unlink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatCurrency } from '@/lib/formatters'
import type { VendorModel } from '@/types/vendor-model'

interface VendorModelItemProps {
  vendorModel: VendorModel
  isBound: boolean
  onToggle: (vendorModel: VendorModel) => void
}

export function VendorModelItem({ vendorModel, isBound, onToggle }: VendorModelItemProps) {
  return (
    <div className="flex items-start justify-between gap-4 p-3 rounded-lg border hover:bg-muted/30 transition-colors">
      <div className="flex-1 space-y-2">
        <div className="flex items-center gap-2 flex-wrap">
          <h4 className="font-medium">
            {vendorModel.vendorModelName}
          </h4>
          <Badge variant="outline" className="text-xs">
            {vendorModel.vendorModelKey}
          </Badge>
          {isBound && (
            <Badge variant="default" className="text-xs">
              已绑定
            </Badge>
          )}
          {!vendorModel.enabled && (
            <Badge variant="secondary" className="text-xs">已禁用</Badge>
          )}
        </div>
        {vendorModel.description && (
          <p className="text-sm text-muted-foreground line-clamp-2">
            {vendorModel.description}
          </p>
        )}
        <div className="flex items-center gap-4 text-sm text-muted-foreground">
          {vendorModel.inputPerMillionTokens && (
            <span>
              输入: {formatCurrency(parseFloat(vendorModel.inputPerMillionTokens), vendorModel.currency || 'USD')} /1M tokens
            </span>
          )}
          {vendorModel.outputPerMillionTokens && (
            <span>
              输出: {formatCurrency(parseFloat(vendorModel.outputPerMillionTokens), vendorModel.currency || 'USD')} /1M tokens
            </span>
          )}
        </div>
      </div>
      <Button
        size="sm"
        variant={isBound ? "outline" : "default"}
        onClick={() => onToggle(vendorModel)}
        disabled={!vendorModel.enabled}
        className="shrink-0"
      >
        {isBound ? (
          <>
            <Unlink className="h-3 w-3 mr-1" />
            解绑
          </>
        ) : (
          <>
            <Link className="h-3 w-3 mr-1" />
            绑定
          </>
        )}
      </Button>
    </div>
  )
}