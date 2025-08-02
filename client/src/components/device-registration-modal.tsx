import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/queryClient";
import { useToast } from "@/hooks/use-toast";
import { isUnauthorizedError } from "@/lib/authUtils";
import { InfoIcon, Search, Smartphone, Plus, Edit } from "lucide-react";
import { useState, useEffect } from "react";

const deviceRegistrationSchema = z.object({
  childName: z.string()
    .min(2, "Child name must be at least 2 characters")
    .max(50, "Child name must be less than 50 characters")
    .regex(/^[a-zA-Z\s]+$/, "Child name can only contain letters and spaces"),

  name: z.string()
    .min(3, "Device name must be at least 3 characters")
    .max(50, "Device name must be less than 50 characters"),
  imei: z.string()
    .min(14, "IMEI must be at least 14 digits")
    .max(17, "IMEI must be less than 17 characters")
    .regex(/^\d+$/, "IMEI can only contain digits"),
  countryCode: z.string().min(1, "Please select a country code"),
  phoneNumber: z.string()
    .min(7, "Phone number must be at least 7 digits")
    .max(15, "Phone number must be less than 15 digits")
    .regex(/^\d+$/, "Phone number can only contain digits"),
  deviceType: z.enum(["mobile", "tablet"], {
    required_error: "Please select a device type",
  }),
  model: z.string().max(50, "Model name must be less than 50 characters").optional(),
});

type DeviceRegistrationForm = z.infer<typeof deviceRegistrationSchema>;

interface DeviceRegistrationModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const countryCodes = [
  { code: "+91", country: "India", flag: "🇮🇳" },
  { code: "+1", country: "United States", flag: "🇺🇸" },
  { code: "+44", country: "United Kingdom", flag: "🇬🇧" },
  { code: "+33", country: "France", flag: "🇫🇷" },
  { code: "+49", country: "Germany", flag: "🇩🇪" },
  { code: "+81", country: "Japan", flag: "🇯🇵" },
  { code: "+86", country: "China", flag: "🇨🇳" },
  { code: "+7", country: "Russia", flag: "🇷🇺" },
  { code: "+55", country: "Brazil", flag: "🇧🇷" },
  { code: "+61", country: "Australia", flag: "🇦🇺" },
];

export function DeviceRegistrationModal({ open, onOpenChange }: DeviceRegistrationModalProps) {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [isDeviceRegistered, setIsDeviceRegistered] = useState(false);
  const [editMode, setEditMode] = useState<'none' | 'lookup' | 'editing' | 'confirm'>('none');
  const [editData, setEditData] = useState<any>(null);
  const [editLookupPhone, setEditLookupPhone] = useState("");
  const [editLookupCountryCode, setEditLookupCountryCode] = useState("+91");
  const [newPhoneNumber, setNewPhoneNumber] = useState("");

  const { data: children } = useQuery({
    queryKey: ["/api/children"],
    retry: false,
  });

  const form = useForm<DeviceRegistrationForm>({
    resolver: zodResolver(deviceRegistrationSchema),
    defaultValues: {
      childName: "",
      name: "",
      imei: "",
      countryCode: "+91",
      phoneNumber: "",
      deviceType: "mobile",
      model: "",
    },
  });

  // Track form changes to show/hide Add button
  const watchedValues = form.watch();
  


  // Reset state when modal is closed
  useEffect(() => {
    if (!open) {
      setIsDeviceRegistered(false);
      setEditMode('none');
      setEditData(null);
      setEditLookupPhone("");
      setNewPhoneNumber("");
      form.reset();
    }
  }, [open, form]);

  // Lookup existing device by phone number for editing
  const editLookupMutation = useMutation({
    mutationFn: async ({ countryCode, phoneNumber }: { countryCode: string; phoneNumber: string }) => {
      const fullPhone = `${countryCode} ${phoneNumber}`;
      const encodedPhone = encodeURIComponent(fullPhone);
      const response = await apiRequest("GET", `/api/devices/lookup/mobile/${encodedPhone}`);
      return response.json();
    },
    onSuccess: (data) => {
      if (data.device) {
        setEditData(data.device);
        setEditMode('editing');
        toast({
          title: "Device Found",
          description: `Found device: ${data.device.name} for child ${data.device.childName}`,
        });
      } else {
        toast({
          title: "Device Not Found",
          description: "No registered device found with this mobile number.",
          variant: "destructive",
        });
      }
    },
    onError: (error) => {
      toast({
        title: "Lookup Failed",
        description: "Failed to lookup device by mobile number.",
        variant: "destructive",
      });
    },
  });

  // Update device phone number
  const updateDeviceMutation = useMutation({
    mutationFn: async ({ deviceId, newPhone }: { deviceId: number; newPhone: string }) => {
      const response = await apiRequest("PATCH", `/api/devices/${deviceId}`, { 
        phoneNumber: newPhone 
      });
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/api/devices"] });
      toast({
        title: "Device Updated",
        description: "Mobile number updated successfully.",
      });
      setEditMode('none');
      setEditData(null);
      onOpenChange(false);
    },
    onError: (error) => {
      toast({
        title: "Update Failed",
        description: "Failed to update device mobile number.",
        variant: "destructive",
      });
    },
  });

  const [showAddMobileForm, setShowAddMobileForm] = useState(false);
  const [addMobileData, setAddMobileData] = useState({
    deviceName: "",
    childName: ""
  });

  const addMobileMutation = useMutation({
    mutationFn: async (data: { phoneNumber: string, imei: string, deviceName: string, childName?: string }) => {
      const response = await apiRequest("POST", "/api/devices/add-mobile", data);
      return await response.json();
    },
    onSuccess: (data) => {
      toast({
        title: "Mobile Number Added",
        description: "Mobile number successfully added to database. You can now use IMEI lookup.",
      });
      setShowAddMobileForm(false);
      setAddMobileData({ deviceName: "", childName: "" });

    },
    onError: (error) => {
      if (isUnauthorizedError(error)) {
        toast({
          title: "Unauthorized",
          description: "You are logged out. Logging in again...",
          variant: "destructive",
        });
        setTimeout(() => {
          window.location.href = "/api/login";
        }, 500);
        return;
      }
      toast({
        title: "Error Adding Mobile",
        description: "Failed to add mobile number to database. Please try again.",
        variant: "destructive",
      });
    },
  });



  const createChildMutation = useMutation({
    mutationFn: async (data: { name: string }) => {
      const response = await apiRequest("POST", "/api/children", data);
      return response.json();
    },
    onError: (error) => {
      if (isUnauthorizedError(error)) {
        toast({
          title: "Unauthorized",
          description: "You are logged out. Logging in again...",
          variant: "destructive",
        });
        setTimeout(() => {
          window.location.href = "/api/login";
        }, 500);
        return;
      }
      toast({
        title: "Error",
        description: "Failed to create child profile",
        variant: "destructive",
      });
    },
  });

  const deviceRegistrationMutation = useMutation({
    mutationFn: async (data: DeviceRegistrationForm) => {
      console.log("Starting device registration with data:", data);
      
      // First, create child if needed
      let childId;
      const existingChild = children?.find((c: any) => c.name === data.childName);
      
      if (existingChild) {
        childId = existingChild.id;
        console.log("Using existing child:", existingChild);
      } else {
        console.log("Creating new child profile");
        const child = await createChildMutation.mutateAsync({
          name: data.childName,
        });
        childId = child.id;
        console.log("Created new child:", child);
      }

      // Then create device
      const deviceData = {
        childId,
        name: data.name,
        imei: "PENDING_IMEI_LOOKUP", // Will be set when device connects
        phoneNumber: `${data.countryCode} ${data.phoneNumber}`,
        deviceType: data.deviceType,
        model: data.model,
      };

      console.log("Sending device data to API:", deviceData);
      const response = await apiRequest("POST", "/api/devices", deviceData);
      const result = await response.json();
      console.log("Device registration response:", result);
      return result;
    },
    onSuccess: async (response) => {
      const data = form.getValues();
      
      // Try to fetch IMEI for the registered mobile number
      try {
        const phoneNumber = `${data.countryCode} ${data.phoneNumber}`;
        const encodedPhone = encodeURIComponent(phoneNumber);
        const imeiResponse = await apiRequest("GET", `/api/devices/lookup/mobile/${encodedPhone}`);
        const imeiData = await imeiResponse.json();
        
        if (imeiData.imei && imeiData.imei !== "PENDING_IMEI_LOOKUP") {
          // Update the device with the retrieved IMEI
          await apiRequest("PATCH", `/api/devices/${response.device.id}`, { imei: imeiData.imei });
          
          // Update the form field to show the populated IMEI
          form.setValue("imei", imeiData.imei);
          
          toast({
            title: "Device Added & IMEI Retrieved",
            description: `${data.name} registered with IMEI ${imeiData.imei} for ${data.childName}.`,
          });
          
          // Mark device as registered to hide Add button
          setIsDeviceRegistered(true);
          setInitialFormValues(data);
          
          // Don't close modal immediately - let user see the IMEI population
          setTimeout(() => {
            onOpenChange(false);
            form.reset();
            setIsDeviceRegistered(false);
            setInitialFormValues(null);
          }, 4000);
          
        } else {
          // Normal success flow without IMEI
          if (response.isExistingDevice) {
            toast({
              title: "Device Linked Successfully!",
              description: response.message || "This device was already registered. Parental controls will work immediately.",
              className: "border-l-4 border-success-green",
            });
          } else {
            toast({
              title: "Device Registration Sent",
              description: response.message || "A pairing request has been sent to the child's device for approval",
            });
          }
          
          toast({
            title: "Device Registered Successfully!",
            description: "Device has been registered. Child can now connect using Knets Jr app.",
            className: "border-l-4 border-success-green",
          });
          
          // Close modal after successful registration
          onOpenChange(false);
        }
      } catch (error) {
        // IMEI lookup failed, show normal success message
        if (response.isExistingDevice) {
          toast({
            title: "Device Linked Successfully!",
            description: response.message || "This device was already registered. Parental controls will work immediately.",
            className: "border-l-4 border-success-green",
          });
        } else {
          toast({
            title: "Device Registration Sent",
            description: response.message || "A pairing request has been sent to the child's device for approval. Modal stays open for IMEI lookup.",
            className: "border-l-4 border-success-green",
          });
        }
        
        // Close modal after successful registration
        onOpenChange(false);
      }
      
      queryClient.invalidateQueries({ queryKey: ["/api/devices"] });
      queryClient.invalidateQueries({ queryKey: ["/api/children"] });
      queryClient.invalidateQueries({ queryKey: ["/api/dashboard/stats"] });
    },
    onError: (error) => {
      if (isUnauthorizedError(error)) {
        toast({
          title: "Unauthorized",
          description: "You are logged out. Logging in again...",
          variant: "destructive",
        });
        setTimeout(() => {
          window.location.href = "/api/login";
        }, 500);
        return;
      }
      toast({
        title: "Error",
        description: "Failed to register device",
        variant: "destructive",
      });
    },
  });

  const onSubmit = (data: DeviceRegistrationForm) => {
    console.log("Form submitted with data:", data);
    deviceRegistrationMutation.mutate(data);
  };



  // Check if form is valid for submit button
  const isFormValid = () => {
    const { childName, name, imei, countryCode, phoneNumber, deviceType } = form.getValues();
    return childName.length >= 2 && 
           name.length >= 3 && 
           imei.length >= 14 &&
           countryCode && 
           phoneNumber.length >= 7 && 
           deviceType;
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md max-h-[85vh] flex flex-col overflow-hidden sm:max-h-[90vh]">
        <DialogHeader className="flex-shrink-0">
          <DialogTitle>Add New Device</DialogTitle>
          <DialogDescription>
            Register a new device for monitoring and control
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto pr-1 -mr-1">
          {/* Edit Mode UI */}
          {editMode === 'lookup' && (
            <div className="space-y-4 pb-4">
              <div className="text-center mb-6">
                <h3 className="text-lg font-semibold text-gray-900">Edit Device Details</h3>
                <p className="text-sm text-gray-600 mt-2">Enter the current registered mobile number</p>
              </div>
              
              <div className="grid grid-cols-4 gap-3">
                <div className="col-span-1">
                  <Label>Country</Label>
                  <Select value={editLookupCountryCode} onValueChange={setEditLookupCountryCode}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="+91">🇮🇳 +91</SelectItem>
                      <SelectItem value="+1">🇺🇸 +1</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="col-span-3">
                  <Label>Current Mobile Number</Label>
                  <Input
                    placeholder="Enter registered mobile number"
                    value={editLookupPhone}
                    onChange={(e) => setEditLookupPhone(e.target.value.replace(/\D/g, ''))}
                  />
                </div>
              </div>
              
              <div className="flex justify-center gap-3 mt-6">
                <Button variant="outline" onClick={() => setEditMode('none')}>Cancel</Button>
                <Button
                  onClick={() => editLookupMutation.mutate({
                    countryCode: editLookupCountryCode,
                    phoneNumber: editLookupPhone
                  })}
                  disabled={!editLookupPhone || editLookupMutation.isPending}
                >
                  {editLookupMutation.isPending ? "Looking up..." : "Find Device"}
                </Button>
              </div>
            </div>
          )}

          {editMode === 'editing' && editData && (
            <div className="space-y-4 pb-4">
              <div className="text-center mb-6">
                <h3 className="text-lg font-semibold text-gray-900">Edit Device Details</h3>
              </div>
              
              <div className="bg-blue-50 p-4 rounded-lg mb-4">
                <h4 className="font-medium text-trust-blue mb-2">Current Details</h4>
                <p><strong>Child:</strong> {editData.childName}</p>
                <p><strong>Device:</strong> {editData.name}</p>
                <p><strong>Number:</strong> {editData.phoneNumber}</p>
              </div>
              
              <div className="grid grid-cols-4 gap-3">
                <div className="col-span-1">
                  <Label>Country</Label>
                  <Select value={editLookupCountryCode} onValueChange={setEditLookupCountryCode}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="+91">🇮🇳 +91</SelectItem>
                      <SelectItem value="+1">🇺🇸 +1</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="col-span-3">
                  <Label>New Mobile Number</Label>
                  <Input
                    placeholder="Enter new number"
                    value={newPhoneNumber}
                    onChange={(e) => setNewPhoneNumber(e.target.value.replace(/\D/g, ''))}
                  />
                </div>
              </div>
              
              <div className="flex justify-center gap-3 mt-6">
                <Button variant="outline" onClick={() => setEditMode('none')}>Cancel</Button>
                <Button
                  onClick={() => setEditMode('confirm')}
                  disabled={!newPhoneNumber}
                >
                  Continue
                </Button>
              </div>
            </div>
          )}

          {editMode === 'confirm' && editData && (
            <div className="space-y-4 pb-4">
              <div className="text-center mb-6">
                <h3 className="text-lg font-semibold text-gray-900">Confirm Changes</h3>
              </div>
              
              <div className="bg-yellow-50 p-4 rounded-lg">
                <h4 className="font-medium mb-2">Confirm Number Change</h4>
                <p><strong>Child:</strong> {editData.childName}</p>
                <p><strong>Device:</strong> {editData.name}</p>
                <p><strong>From:</strong> <span className="text-red-600">{editData.phoneNumber}</span></p>
                <p><strong>To:</strong> <span className="text-green-600">{editLookupCountryCode} {newPhoneNumber}</span></p>
              </div>
              
              <div className="flex justify-center gap-3 mt-6">
                <Button variant="outline" onClick={() => setEditMode('none')}>No, Discard</Button>
                <Button
                  onClick={() => {
                    updateDeviceMutation.mutate({
                      deviceId: editData.id,
                      newPhone: `${editLookupCountryCode} ${newPhoneNumber}`
                    });
                  }}
                  disabled={updateDeviceMutation.isPending}
                  className="bg-green-600 hover:bg-green-700"
                >
                  {updateDeviceMutation.isPending ? "Saving..." : "Yes, Save Changes"}
                </Button>
              </div>
            </div>
          )}

          {/* Normal Registration Form */}
          {editMode === 'none' && (
            <Form {...form}>
              <form id="device-registration-form" onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pb-4">
            <FormField
              control={form.control}
              name="childName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Child's Name *</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder="Enter child's full name (e.g., Emma Smith)" 
                      {...field}
                      onChange={(e) => {
                        // Only allow letters and spaces
                        const value = e.target.value.replace(/[^a-zA-Z\s]/g, '');
                        field.onChange(value);
                      }}
                      maxLength={50}
                    />
                  </FormControl>
                  <FormDescription>
                    Letters and spaces only, 2-50 characters
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />



            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Device Name *</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder="e.g., Emma's iPhone, Dad's Tablet" 
                      {...field}
                      maxLength={50}
                    />
                  </FormControl>
                  <FormDescription>
                    3-50 characters, describe this specific device
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="deviceType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Device Type</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select device type" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="mobile">Mobile Phone</SelectItem>
                      <SelectItem value="tablet">Tablet</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="model"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Device Model (Optional)</FormLabel>
                  <FormControl>
                    <Input placeholder="e.g., iPhone 13, Galaxy S23" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-4 gap-3">
              <FormField
                control={form.control}
                name="countryCode"
                render={({ field }) => (
                  <FormItem className="col-span-1">
                    <FormLabel>Country *</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Code" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="+91">🇮🇳 +91</SelectItem>
                        <SelectItem value="+1">🇺🇸 +1</SelectItem>
                        <SelectItem value="+44">🇬🇧 +44</SelectItem>
                        <SelectItem value="+33">🇫🇷 +33</SelectItem>
                        <SelectItem value="+49">🇩🇪 +49</SelectItem>
                        <SelectItem value="+39">🇮🇹 +39</SelectItem>
                        <SelectItem value="+34">🇪🇸 +34</SelectItem>
                        <SelectItem value="+31">🇳🇱 +31</SelectItem>
                        <SelectItem value="+86">🇨🇳 +86</SelectItem>
                        <SelectItem value="+81">🇯🇵 +81</SelectItem>
                        <SelectItem value="+82">🇰🇷 +82</SelectItem>
                        <SelectItem value="+61">🇦🇺 +61</SelectItem>
                        <SelectItem value="+55">🇧🇷 +55</SelectItem>
                        <SelectItem value="+52">🇲🇽 +52</SelectItem>
                        <SelectItem value="+7">🇷🇺 +7</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="phoneNumber"
                render={({ field }) => (
                  <FormItem className="col-span-3">
                    <FormLabel>Mobile Number *</FormLabel>
                    <FormControl>
                      <Input 
                        placeholder="Enter phone number (e.g., 8870929411)" 
                        {...field}
                        onChange={(e) => {
                          // Only allow digits
                          const value = e.target.value.replace(/\D/g, '');
                          if (value.length <= 15) {
                            field.onChange(value);
                          }
                        }}
                        onFocus={(e) => {
                          // Scroll the field into view on mobile
                          setTimeout(() => {
                            e.target.scrollIntoView({ behavior: 'smooth', block: 'center' });
                          }, 300);
                        }}
                        maxLength={15}
                      />
                    </FormControl>
                    <FormDescription>
                      7-15 digits only, without country code
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="imei"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Device IMEI *</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder="Enter device IMEI (ask child to dial *#06#)" 
                      {...field}
                      onChange={(e) => {
                        // Only allow digits
                        const value = e.target.value.replace(/\D/g, '');
                        if (value.length <= 17) {
                          field.onChange(value);
                        }
                      }}
                      maxLength={17}
                    />
                  </FormControl>
                  <FormDescription>
                    Ask your child to dial *#06# to get their device IMEI (14-15 digits)
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="flex justify-center mt-6">
              <Button 
                type="submit" 
                disabled={!isFormValid() || deviceRegistrationMutation.isPending}
                className="px-8 py-2 bg-trust-blue hover:bg-trust-blue-dark text-white font-medium rounded-lg transition-colors"
              >
                {deviceRegistrationMutation.isPending ? "Adding Device..." : "Add Device"}
              </Button>
            </div>
            




            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-start space-x-3">
                <InfoIcon className="w-5 h-5 text-trust-blue mt-0.5" />
                <div>
                  <h4 className="text-sm font-medium text-trust-blue">Next Steps</h4>
                  <p className="text-xs text-blue-700 mt-1">
                    After adding the device, ask your child to install Knets Jr app and enter their mobile number. 
                    The app will automatically connect to this registration and retrieve the IMEI.
                  </p>
                  <p className="text-xs text-blue-600 mt-2">
                    Child consent may be required before parental controls activate.
                  </p>
                </div>
              </div>
            </div>

              </form>
            </Form>
          )}
        </div>

        <div className="flex-shrink-0 border-t pt-4 mt-4">
          <div className="flex justify-center">
            <Button 
              type="button" 
              variant="outline" 
              className="px-8 py-2"
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
