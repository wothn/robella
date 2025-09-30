import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Plus, Trash2, Calculator } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { api } from '@/lib/api';

interface PricingTier {
  id?: number;
  vendorModelId: number;
  tierNumber: number;
  minTokens: number;
  maxTokens: number;
  inputPrice: number;
  outputPrice: number;
  cachedInputPrice: number;
  currency: string;
}

interface VendorModel {
  id: number;
  modelName: string;
  providerName: string;
  pricingStrategy: 'FIXED' | 'TIERED' | 'PER_REQUEST';
}

interface CostCalculation {
  inputTokens: number;
  cachedTokens: number;
  outputTokens: number;
  inputCost: number;
  outputCost: number;
  totalCost: number;
  currency: string;
  pricingStrategy: string;
}

export default function PricingManagement() {
  const { toast } = useToast();
  const [vendorModels, setVendorModels] = useState<VendorModel[]>([]);
  const [selectedModel, setSelectedModel] = useState<VendorModel | null>(null);
  const [pricingTiers, setPricingTiers] = useState<PricingTier[]>([]);
  const [loading, setLoading] = useState(false);
  const [calculation, setCalculation] = useState<CostCalculation | null>(null);
  const [testTokens, setTestTokens] = useState({
    input: 1000000,
    cached: 0,
    output: 500000
  });

  useEffect(() => {
    fetchVendorModels();
  }, []);

  const fetchVendorModels = async () => {
    try {
      const response = await api.get('/api/vendor-models');
      setVendorModels(response.data);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to fetch vendor models',
        variant: 'destructive'
      });
    }
  };

  const fetchPricingTiers = async (vendorModelId: number) => {
    try {
      const response = await api.get(`/api/pricing/tiers/${vendorModelId}`);
      setPricingTiers(response.data);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to fetch pricing tiers',
        variant: 'destructive'
      });
    }
  };

  const handleModelSelect = async (model: VendorModel) => {
    setSelectedModel(model);
    setLoading(true);
    await fetchPricingTiers(model.id);
    setLoading(false);
  };

  const addPricingTier = () => {
    if (!selectedModel) return;
    
    const newTier: PricingTier = {
      vendorModelId: selectedModel.id,
      tierNumber: pricingTiers.length + 1,
      minTokens: pricingTiers.length === 0 ? 0 : pricingTiers[pricingTiers.length - 1].maxTokens + 1,
      maxTokens: pricingTiers.length === 0 ? 1000000 : pricingTiers[pricingTiers.length - 1].maxTokens * 2,
      inputPrice: 0.01,
      outputPrice: 0.03,
      cachedInputPrice: 0.005,
      currency: 'USD'
    };
    setPricingTiers([...pricingTiers, newTier]);
  };

  const updateTier = (index: number, field: keyof PricingTier, value: any) => {
    const updated = [...pricingTiers];
    updated[index] = { ...updated[index], [field]: value };
    setPricingTiers(updated);
  };

  const removeTier = (index: number) => {
    const updated = pricingTiers.filter((_, i) => i !== index);
    // 重新排序层级编号
    const reordered = updated.map((tier, i) => ({ ...tier, tierNumber: i + 1 }));
    setPricingTiers(reordered);
  };

  const savePricingTiers = async () => {
    if (!selectedModel) return;
    
    try {
      await api.post(`/api/pricing/tiers/${selectedModel.id}`, pricingTiers);
      toast({
        title: 'Success',
        description: 'Pricing tiers saved successfully'
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to save pricing tiers',
        variant: 'destructive'
      });
    }
  };

  const calculateCost = async () => {
    if (!selectedModel) return;
    
    try {
      const response = await api.get(
        `/api/pricing/calculate/${selectedModel.id}?inputTokens=${testTokens.input}&cachedTokens=${testTokens.cached}&outputTokens=${testTokens.output}`
      );
      setCalculation(response.data);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to calculate cost',
        variant: 'destructive'
      });
    }
  };

  const updateModelStrategy = async (strategy: 'FIXED' | 'TIERED' | 'PER_REQUEST') => {
    if (!selectedModel) return;
    
    try {
      await api.put(`/api/vendor-models/${selectedModel.id}/strategy`, { pricingStrategy: strategy });
      setSelectedModel({ ...selectedModel, pricingStrategy: strategy });
      toast({
        title: 'Success',
        description: 'Pricing strategy updated successfully'
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to update pricing strategy',
        variant: 'destructive'
      });
    }
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Pricing Management</CardTitle>
          <CardDescription>Manage pricing tiers for AI models</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <Label>Select Vendor Model</Label>
              <Select
                value={selectedModel?.id.toString()}
                onValueChange={(value) => {
                  const model = vendorModels.find(m => m.id.toString() === value);
                  if (model) handleModelSelect(model);
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Choose a vendor model" />
                </SelectTrigger>
                <SelectContent>
                  {vendorModels.map(model => (
                    <SelectItem key={model.id} value={model.id.toString()}>
                      {model.modelName} - {model.providerName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {selectedModel && (
              <div className="space-y-4">
                <div>
                  <Label>Pricing Strategy</Label>
                  <Select
                    value={selectedModel.pricingStrategy}
                    onValueChange={updateModelStrategy}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="FIXED">Fixed Pricing</SelectItem>
                      <SelectItem value="TIERED">Tiered Pricing</SelectItem>
                      <SelectItem value="PER_REQUEST">Per Request Pricing</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {selectedModel.pricingStrategy === 'TIERED' && (
                  <div className="space-y-4">
                    <div className="flex justify-between items-center">
                      <h3 className="text-lg font-semibold">Pricing Tiers</h3>
                      <Button onClick={addPricingTier} size="sm">
                        <Plus className="h-4 w-4 mr-2" />
                        Add Tier
                      </Button>
                    </div>

                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Tier</TableHead>
                          <TableHead>Min Tokens</TableHead>
                          <TableHead>Max Tokens</TableHead>
                          <TableHead>Input Price</TableHead>
                          <TableHead>Output Price</TableHead>
                          <TableHead>Cached Price</TableHead>
                          <TableHead>Actions</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {pricingTiers.map((tier, index) => (
                          <TableRow key={index}>
                            <TableCell>{tier.tierNumber}</TableCell>
                            <TableCell>
                              <Input
                                type="number"
                                value={tier.minTokens}
                                onChange={(e) => updateTier(index, 'minTokens', parseInt(e.target.value))}
                                className="w-24"
                              />
                            </TableCell>
                            <TableCell>
                              <Input
                                type="number"
                                value={tier.maxTokens}
                                onChange={(e) => updateTier(index, 'maxTokens', parseInt(e.target.value))}
                                className="w-24"
                              />
                            </TableCell>
                            <TableCell>
                              <Input
                                type="number"
                                step="0.0001"
                                value={tier.inputPrice}
                                onChange={(e) => updateTier(index, 'inputPrice', parseFloat(e.target.value))}
                                className="w-24"
                              />
                            </TableCell>
                            <TableCell>
                              <Input
                                type="number"
                                step="0.0001"
                                value={tier.outputPrice}
                                onChange={(e) => updateTier(index, 'outputPrice', parseFloat(e.target.value))}
                                className="w-24"
                              />
                            </TableCell>
                            <TableCell>
                              <Input
                                type="number"
                                step="0.0001"
                                value={tier.cachedInputPrice}
                                onChange={(e) => updateTier(index, 'cachedInputPrice', parseFloat(e.target.value))}
                                className="w-24"
                              />
                            </TableCell>
                            <TableCell>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => removeTier(index)}
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>

                    <Button onClick={savePricingTiers} disabled={loading}>
                      Save Pricing Tiers
                    </Button>
                  </div>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {selectedModel && (
        <Card>
          <CardHeader>
            <CardTitle>Cost Calculator</CardTitle>
            <CardDescription>Test cost calculation with current pricing</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-3 gap-4 mb-4">
              <div>
                <Label>Input Tokens</Label>
                <Input
                  type="number"
                  value={testTokens.input}
                  onChange={(e) => setTestTokens({...testTokens, input: parseInt(e.target.value)})}
                />
              </div>
              <div>
                <Label>Cached Tokens</Label>
                <Input
                  type="number"
                  value={testTokens.cached}
                  onChange={(e) => setTestTokens({...testTokens, cached: parseInt(e.target.value)})}
                />
              </div>
              <div>
                <Label>Output Tokens</Label>
                <Input
                  type="number"
                  value={testTokens.output}
                  onChange={(e) => setTestTokens({...testTokens, output: parseInt(e.target.value)})}
                />
              </div>
            </div>

            <Button onClick={calculateCost} className="mb-4">
              <Calculator className="h-4 w-4 mr-2" />
              Calculate Cost
            </Button>

            {calculation && (
              <div className="bg-gray-50 p-4 rounded-lg">
                <h4 className="font-semibold mb-2">Cost Breakdown</h4>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-600">Input Cost:</span>
                    <span className="ml-2 font-mono">
                      {calculation.inputCost.toFixed(6)} {calculation.currency}
                    </span>
                  </div>
                  <div>
                    <span className="text-gray-600">Output Cost:</span>
                    <span className="ml-2 font-mono">
                      {calculation.outputCost.toFixed(6)} {calculation.currency}
                    </span>
                  </div>
                  <div>
                    <span className="text-gray-600">Total Cost:</span>
                    <span className="ml-2 font-mono font-bold">
                      {calculation.totalCost.toFixed(6)} {calculation.currency}
                    </span>
                  </div>
                  <div>
                    <span className="text-gray-600">Strategy:</span>
                    <Badge variant="outline" className="ml-2">
                      {calculation.pricingStrategy}
                    </Badge>
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}